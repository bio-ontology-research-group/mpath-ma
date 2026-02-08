import java.io.File

// For each class, we want to know if it's over-represented in a Strain or Sex, controlling for Age.
// We'll bin age into 3 groups: Young (<400), Middle (400-700), Old (>700).

def mice = [:] // MouseID -> [Strain, Sex, AgeBin, Classes: Set]
def ageBins = ['Young', 'Middle', 'Old']

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
def allClasses = mice.values().collectMany { it.classes }.unique()

def totalMicePerGroup = [:].withDefault { 0 }
def classCountPerGroup = [:].withDefault { 0 }
def totalMicePerAgeBin = [:].withDefault { 0 }
def classCountPerAgeBin = [:].withDefault { 0 }

mice.each { id, data ->
    totalMicePerGroup["Strain:${data.strain}|${data.ageBin}"]++
    totalMicePerGroup["Sex:${data.sex}|${data.ageBin}"]++
    totalMicePerAgeBin[data.ageBin]++
    
    data.classes.each { cls ->
        classCountPerGroup["${cls}|Strain:${data.strain}|${data.ageBin}"]++
        classCountPerGroup["${cls}|Sex:${data.sex}|${data.ageBin}"]++
        classCountPerAgeBin["${cls}|${data.ageBin}"]++
    }
}

def results = []

def analyzeFactor = { factorName, factorValues ->
    allClasses.each { cls ->
        factorValues.each { val ->
            double observed = 0
            double expected = 0
            
            ageBins.each { bin ->
                int n_group_bin = totalMicePerGroup["${factorName}:${val}|${bin}"]
                int N_bin = totalMicePerAgeBin[bin]
                int C_bin = classCountPerAgeBin["${cls}|${bin}"]
                
                if (N_bin > 0) {
                    observed += classCountPerGroup["${cls}|${factorName}:${val}|${bin}"]
                    expected += (double) n_group_bin * C_bin / N_bin
                }
            }
            
            if (observed > expected && observed >= 5) {
                double ratio = observed / expected
                results << [Factor: factorName, Value: val, Class: cls, Observed: observed, Expected: String.format("%.2f", expected), Ratio: ratio]
            }
        }
    }
}

analyzeFactor("Strain", allStrains)
analyzeFactor("Sex", allSexes)

results.sort { - it.Ratio }

def out = new File('analysis/results.tsv')
out.withWriter { w ->
    w.write("Factor\tValue\tClass\tObserved\tExpected\tRatio\n")
    results.each { r ->
        w.write("${r.Factor}\t${r.Value}\t${r.Class}\t${r.Observed}\t${r.Expected}\t${String.format("%.2f", r.Ratio)}\n")
    }
}

println "Results written to analysis/results.tsv"
println "Top 10 results:"
results.take(10).each { r ->
    println "${r.Factor} | ${r.Value} | ${r.Class} | Obs: ${r.Observed} | Exp: ${r.Expected} | Ratio: ${String.format("%.2f", r.Ratio)}"
}