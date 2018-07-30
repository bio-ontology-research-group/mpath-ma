
import java.io.File;


preMouseID = 0
listOfStrains = ["129S1/SvImJ","A/J","BALB/cByJ","BTBR T<+> tf/J","BUB/BnJ","C3H/HeJ","C57BL/10J","C57BL/6J","C57BLKS/J","C57BR/cdJ","C57L/J","CBA/J","DBA/2J","FVB/NJ","KK/HlJ","LP/J","MRL/MpJ","NOD.B10Sn-H2<b>/J","NON/ShiLtJ","NZO/H1LtJ","NZW/LaCJ","P/J","PL/J","PWD/PhJ","RIIIS/J","SM/J","SWR/J","WSB/EiJ"]
ls = [["129S1/SvImJ"],["A/J"],["BALB/cByJ"],["BTBR T<+> tf/J"],["BUB/BnJ"],["C3H/HeJ"],["C57BL/10J"],["C57BL/6J"],["C57BLKS/J"],["C57BR/cdJ"],["C57L/J"],["CBA/J"],["DBA/2J"],["FVB/NJ"],["KK/HlJ"],["LP/J"],["MRL/MpJ"],["NOD.B10Sn-H2<b>/J"],["NON/ShiLtJ"],["NZO/H1LtJ"],["NZW/LaCJ"],["P/J"],["PL/J"],["PWD/PhJ"],["RIIIS/J"],["SM/J"],["SWR/J"],["WSB/EiJ"]]
lsOfSex = ['F','M']
lss = [ls,ls]//F,M
lsOfAge = ['20M','12M']
lsa = [ls,ls] //20,12
lssa = [ls,ls,ls,ls] //F20,F12,M20,M12

new File("completeDataID.csv").splitEachLine(",") { line ->
	if((line[0]!=null)&&(listOfStrains.contains(line[3]))&&(lsOfAge.contains(line[17])))
	{
		mouseID = line[0]
		mouseStrain = line[3]
		o = "MAP:00000010"
		age = line[17]
		sex = line[4]
		s =(sex==lsOfSex[0])?0:1;
		a = (age==lsOfAge[0])?0:1;
		sa = (2*s)+a

		if(((line[10]!="")&&(line[10]!=" ")&&(line[10]!=null))  && ((line[7]!="")&&(line[7]!=" ")&&(line[7]!=null)) /*&& (line[4]=="M") && (line[17]=="20M" || line[17]=="12M")*/)
		{
			mpathID = line[10].replaceAll(":","_")
			maID = line[7].replaceAll(":","_")

			for (j=0; j<28; j++)
			{
				if(mouseStrain.toLowerCase() == listOfStrains[j].toLowerCase())
				{
					lss[s][j].add(mouseID)
					lss[s][j].add('\t')
					lss[s][j].add("MAP:"+maID.replaceAll("MA_","")+mpathID.replaceAll("MPATH_",""))
					lss[s][j].add('\t')
					lss[s][j].add(1)
					lss[s][j].add('\n')

					lsa[a][j].add(mouseID)
					lsa[a][j].add('\t')
					lsa[a][j].add("MAP:"+maID.replaceAll("MA_","")+mpathID.replaceAll("MPATH_",""))
					lsa[a][j].add('\t')
					lsa[a][j].add(1)
					lsa[a][j].add('\n')

					lssa[sa][j].add(mouseID)
					lssa[sa][j].add('\t')
					lssa[sa][j].add("MAP:"+maID.replaceAll("MA_","")+mpathID.replaceAll("MPATH_",""))
					lssa[sa][j].add('\t')
					lssa[sa][j].add(1)
					lssa[sa][j].add('\n')

					for (i=0; i<28; i++)
					{
						if(i!= j)
						{
							lss[s][i].add(mouseID)
							lss[s][i].add('\t')
							lss[s][i].add("MAP:"+maID.replaceAll("MA_","")+mpathID.replaceAll("MPATH_",""))
							lss[s][i].add('\t')
							lss[s][i].add(0)
							lss[s][i].add('\n')

							lsa[a][i].add(mouseID)
							lsa[a][i].add('\t')
							lsa[a][i].add("MAP:"+maID.replaceAll("MA_","")+mpathID.replaceAll("MPATH_",""))
							lsa[a][i].add('\t')
							lsa[a][i].add(0)
							lsa[a][i].add('\n')

							lssa[sa][i].add(mouseID)
							lssa[sa][i].add('\t')
							lssa[sa][i].add("MAP:"+maID.replaceAll("MA_","")+mpathID.replaceAll("MPATH_",""))
							lssa[sa][i].add('\t')
							lssa[sa][i].add(0)
							lssa[sa][i].add('\n')
						}
					}
				}

			}
		}
		else if(((line[10]=="")||(line[10]==" ")||(line[10]==null)) && ((line[7]=="")||(line[7]==" ")||(line[7]==null)) /*&& (line[4]=="M") && (line[17]=="20M" || line[17]=="12M")*/)
		{
			for(j=0; j<28; j++)
			{
				if(mouseStrain.toLowerCase() == listOfStrains[j].toLowerCase())
				{
					ls[j].add(mouseID)
					ls[j].add('\t')
					ls[j].add(o)
					ls[j].add('\t')
					ls[j].add(1)
					ls[j].add('\n')


					lss[s][j].add(mouseID)
					lss[s][j].add('\t')
					lss[s][j].add(o)
					lss[s][j].add('\t')
					lss[s][j].add(1)
					lss[s][j].add('\n')

					lsa[a][j].add(mouseID)
					lsa[a][j].add('\t')
					lsa[a][j].add(o)
					lsa[a][j].add('\t')
					lsa[a][j].add(1)
					lsa[a][j].add('\n')

					lssa[sa][j].add(mouseID)
					lssa[sa][j].add('\t')
					lssa[sa][j].add(o)
					lssa[sa][j].add('\t')
					lssa[sa][j].add(1)
					lssa[sa][j].add('\n')

					for (i=0; i<28; i++)
					{
						if(i!= j)
						{
							lss[s][i].add(mouseID)
							lss[s][i].add('\t')
							lss[s][i].add(o)
							lss[s][i].add('\t')
							lss[s][i].add(0)
							lss[s][i].add('\n')

							lsa[a][i].add(mouseID)
							lsa[a][i].add('\t')
							lsa[a][i].add(o)
							lsa[a][i].add('\t')
							lsa[a][i].add(0)
							lsa[a][i].add('\n')

							lssa[sa][i].add(mouseID)
							lssa[sa][i].add('\t')
							lssa[sa][i].add(o)
							lssa[sa][i].add('\t')
							lssa[sa][i].add(0)
							lssa[sa][i].add('\n')
						}
					}
				}

			}
		}

	}
}

sa = 0
for (sa = 0; sa<4; sa++)
{
	for(i = 0; (i<28) &&(sa<2); i++)
	{
		lss[sa][i].remove(0)
		lss[sa][i].pop()
		File lstFile = new File("/home/sarah/groovyWorkSpace/results/files_for_func_fixed_healthy_mice/MAP/groups/"+lsOfSex[sa]+"/i/MAP_"+(listOfStrains[i].replaceAll("/","_")).replaceAll(" T<+> tf","")+"_Func_Annotation.txt")
		lstFile.withWriter{ out ->
		  lss[sa][i].each {out.print it}
		}
	}
	for(i = 0; (i<28) &&(sa<2); i++)
	{
		lsa[sa][i].remove(0)
		lsa[sa][i].pop()
		File lstFile = new File("/home/sarah/groovyWorkSpace/results/files_for_func_fixed_healthy_mice/MAP/groups/"+lsOfAge[sa]+"/i/MAP_"+(listOfStrains[i].replaceAll("/","_")).replaceAll(" T<+> tf","")+"_Func_Annotation.txt")
		lstFile.withWriter{ out ->
		  lsa[sa][i].each {out.print it}
		}
	}

	for(i = 0; (i<28) &&(sa<4); i++)
	{
		lssa[sa][i].remove(0)
		lssa[sa][i].pop()
		File lstFile = new File("/home/sarah/groovyWorkSpace/results/files_for_func_fixed_healthy_mice/MAP/groups/"+lsOfAge[sa%2]+lsOfSex[sa.intdiv(2)] +"/i/MAP_"+(listOfStrains[i].replaceAll("/","_")).replaceAll(" T<+> tf","")+"_Func_Annotation.txt")
		lstFile.withWriter{ out ->
		  lssa[sa][i].each {out.print it}
		}
	}


}