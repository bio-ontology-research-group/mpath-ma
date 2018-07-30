
import java.io.File;

listOfStrains = ["129S1/SvImJ","A/J","BALB/cByJ","BTBR T<+> tf/J","BUB/BnJ","C3H/HeJ","C57BL/10J","C57BL/6J","C57BLKS/J","C57BR/cdJ","C57L/J","CBA/J","DBA/2J","FVB/NJ","KK/HlJ","LP/J","MRL/MpJ","NOD.B10Sn-H2<b>/J","NON/ShiLtJ","NZO/H1LtJ","NZW/LaCJ","P/J","PL/J","PWD/PhJ","RIIIS/J","SM/J","SWR/J","WSB/EiJ"]
preMouseID = 0
ls = []

new File("completeDataID.csv").splitEachLine(",") { line ->
if((line[0]!=null)&&(listOfStrains.contains(line[3])))

{
	mpathID = line[10].replaceAll(":","_")
	maID = line[7].replaceAll(":","_")
	mouseStrain = line[3]
	currentMouseID = line[0]

	if (currentMouseID != preMouseID)
	{
		ls.add('\n')
		ls.add("mouse:"+currentMouseID)
	}
	if(((line[10]!="")&&(line[10]!=" ")&&(line[10]!=null))&&((line[7]!="")&&(line[7]!=" ")&&(line[7]!=null)))
	{
		mpathID = line[10].replaceAll(":","_")
		maID = line[7].replaceAll(":","_")
		preMouseID = currentMouseID
		ls.add('\t')
		ls.add("MAP:"+maID.replaceAll("MA_","")+mpathID.replaceAll("MPATH_",""))
		}
	else
		{
		preMouseID = currentMouseID
		ls.add('\t')
		ls.add("MAP:00000010")
		}

}

}

File lstFile = new File("/home/sarah/groovyWorkSpace/results/similarities_results_fixed_healthy_mice/annotations/MAPAnnotations.txt")
lstFile.withWriter{ out ->
  ls.each {out.print it}

}

//new File("/home/sarah/groovyWorkSpace/MpathMouseData.txt").withWriter('utf-8') { 
//         ls.each{writer-> writer.writeLine it}}

