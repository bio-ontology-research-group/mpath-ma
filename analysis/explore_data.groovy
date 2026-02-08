import java.io.File

def mice = [:] // MouseID -> [Strain, Sex, Age, Classes: Set]
def strains = [:].withDefault { 0 }
def sexes = [:].withDefault { 0 }
def ages = []

new File('mouse_mappings.tsv').eachLine { lineStr, index ->
    if (index == 1) return // Skip header
    def parts = lineStr.split('	')
    if (parts.size() < 6) return
    
    def mouseId = parts[0]
    def strain = parts[1]
    def sex = parts[2]
    def age = parts[3] as Integer
    def ma = parts[4]
    def mpath = parts[5]
    def pam = parts[8].split('/').last()
    
    if (!mice.containsKey(mouseId)) {
        mice[mouseId] = [strain: strain, sex: sex, age: age, classes: [] as Set]
        strains[strain]++
        sexes[sex]++
        ages << age
    }
    mice[mouseId].classes << ma
    mice[mouseId].classes << mpath
    mice[mouseId].classes << pam
}

println "Total unique mice: ${mice.size()}"
println "Strains: ${strains}"
println "Sexes: ${sexes}"
println "Age range: ${ages.min()} to ${ages.max()}"
println "Average age: ${ages.sum() / ages.size()}"
