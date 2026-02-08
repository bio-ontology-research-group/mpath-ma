@Grapes([
    @Grab(group='org.apache.commons', module='commons-math3', version='3.6.1')
])

import org.apache.commons.math3.distribution.HypergeometricDistribution
import java.io.File

def mice = [:] // MouseID -> [Strain, Sex, AgeBin, Classes: Set]
def ageBins = ['Young', 'Middle', 'Old']

// Sex-specific MA IDs to be excluded from sex-based analysis
def sexSpecificMA = [
    'MA:0000384', // ovary
    'MA:0000411', // testis
    'MA:0000389', // uterus
    'MA:0000410', // seminal vesicle
    'MA:0000397', // epididymis
    'MA:0000404', // prostate gland
    'MA:0001702', // clitoral gland
    'MA:0000403', // preputial gland of male
    'MA:0000405', // vagina
    'MA:0000385', // oviduct
    'MA:0000412'  // vas deferens
] as Set

new File('mouse_mappings.tsv').eachLine { lineStr, index ->
    if (index == 1) return
    def parts = lineStr.split('\t')
    if (parts.size() < 9) return
    
    def mouseId = parts[0]
    def strain = parts[1]
    def sex = parts[2]
    def age = parts[3] as Integer
    def ageBin = age < 400 ? 'Young' : (age < 700 ? 'Middle' : 'Old')
    def ma = parts[4]
    def mpath = parts[5]
    def pam = parts[8].split('/').last()
    
    if (!mice.containsKey(mouseId)) {
        mice[mouseId] = [strain: strain, sex: sex, ageBin: ageBin, classes: [] as Set]
    }
    mice[mouseId].classes << ma
    mice[mouseId].classes << mpath
    mice[mouseId].classes << pam
}

def allStrains = mice.values().collect { it.strain }.unique()
def allSexes = ['M', 'F']
def allBoth = mice.values().collect { "${it.strain}_${it.sex}" }.unique()
def allClasses = mice.values().collectMany { it.classes }.unique()

def totalMicePerGroup = [:].withDefault { 0 } 
def classCountPerGroup = [:].withDefault { 0 } 
def totalMicePerAgeBin = [:].withDefault { 0 }
def classCountPerAgeBin = [:].withDefault { 0 } 

mice.each { id, data ->
    def groups = [
        ["Strain", data.strain],
        ["Sex", data.sex],
        ["Both", "${data.strain}_${data.sex}"]
    ]
    
    totalMicePerAgeBin[data.ageBin]++
    
    groups.each { f, v ->
        totalMicePerGroup["${f}|${v}|${data.ageBin}"]++
        data.classes.each { cls ->
            classCountPerGroup["${cls}|${f}|${v}|${data.ageBin}"]++
        }
    }
    
    data.classes.each { cls ->
        classCountPerAgeBin["${cls}|${data.ageBin}"]++
    }
}

def results = []

def runAnalysis = { factorName, factorValues ->
    factorValues.each { val ->
        allClasses.each { cls ->
            // Skip sex-specific organs in sex-based analysis
            if (factorName == "Sex" || factorName == "Both") {
                boolean isTrivial = false
                if (sexSpecificMA.contains(cls)) isTrivial = true
                if (cls.startsWith("PAM_")) {
                    def maPart = "MA:" + cls.substring(4).split("x")[0]
                    if (sexSpecificMA.contains(maPart)) isTrivial = true
                }
                if (isTrivial) return
            }

            def pValues = []
            int totalObserved = 0
            double totalExpected = 0
            
            ageBins.each { bin ->
                int n_group_bin = totalMicePerGroup["${factorName}|${val}|${bin}"]
                if (n_group_bin == 0) return
                
                int N_bin = totalMicePerAgeBin[bin]
                int C_bin = classCountPerAgeBin["${cls}|${bin}"]
                int k_observed = classCountPerGroup["${cls}|${factorName}|${val}|${bin}"]
                
                if (N_bin > 0) {
                    def dist = new HypergeometricDistribution(N_bin, C_bin, n_group_bin)
                    double p = 1.0 - dist.cumulativeProbability(k_observed - 1)
                    pValues << p
                    totalObserved += k_observed
                    totalExpected += (double) n_group_bin * C_bin / N_bin
                }
            }
            
            if (totalObserved >= 5 && pValues) {
                double combinedP = pValues.min() * pValues.size()
                combinedP = Math.min(combinedP, 1.0)
                
                if (combinedP < 0.05) {
                    results << [Factor: factorName, Value: val, Class: cls, Obs: totalObserved, Exp: String.format("%.2f", totalExpected), P: combinedP]
                }
            }
        }
    }
}

println "Analyzing Strains..."
runAnalysis("Strain", allStrains)
println "Analyzing Sex..."
runAnalysis("Sex", allSexes)
println "Analyzing Both..."
runAnalysis("Both", allBoth)

int totalTests = results.size()
results.each { it.AdjP = Math.min(it.P * totalTests, 1.0) }

def significant = results.findAll { it.AdjP < 0.05 }.sort { it.AdjP }

def out = new File('analysis/enrichments_stats.tsv')
out.withWriter { w ->
    w.write("Factor\tValue\tClass\tObserved\tExpected\tP-value\tAdjP-value\n")
    significant.each { r ->
        w.write("${r.Factor}\t${r.Value}\t${r.Class}\t${r.Obs+0.0}\t${r.Exp}\t${String.format("%.2e", r.P)}\t${String.format("%.2e", r.AdjP)}\n")
    }
}

println "Found ${significant.size()} significant enrichments after Bonferroni correction."
significant.take(20).each { r ->
    println "${r.Factor} | ${r.Value} | ${r.Class} | Obs: ${r.Obs} | Exp: ${r.Exp} | AdjP: ${String.format("%.2e", r.AdjP)}"
}
