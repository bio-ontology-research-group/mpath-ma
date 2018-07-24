# Sarah M. Alghamdi

# this code computes the Kendall's tau correlation coefficient between rankings that are generated using different ontologies for the same set of classes
# inputs are found in "EAcode" folder

import scipy.stats as ss

#def namestr(obj, namespace):
#    return [name for name in namespace if namespace[name] is obj]

ls = ["129S1_SvImJ",
"A_J",
"BALB_cByJ",
"BTBR",
"BUB_BnJ",
"C3H_HeJ",
"C57BLKS_J",
"C57BL_10J",
"C57BL_6J",
"C57BR_cdJ",
"C57L_J",
"CBA_J",
"DBA_2J",
"FVB_NJ",
"KK_HlJ",
"LP_J",
"MRL_MpJ",
"NOD",
"NON_ShiLtJ",
"NZO_H1LtJ",
"NZW_LaCJ",
"PL_J",
"PWD_PhJ",
"P_J",
"RIIIS_J",
"SM_J",
"SWR_J",
"WSB_EiJ"]

for strain in range(0,len(ls)):
    strainfile= "EAcode/"+ls[strain]
    straintxt = open(strainfile,'r')
    strainlines = straintxt.read().splitlines()
    strainlines.pop(0)
    strainlines.pop(0)

    PAMT={}
    PAM={}
    MAPT={}
    MAP={}

    dictls=[PAMT,PAM,MAPT,MAP]
    names = ["PAMT","PAM","MAPT","MAP"]

    for  line in strainlines:
        linels = line.split('\t')
        for i in range(0,len(dictls)):
            if(len(linels)>11):
                code = linels[2*i]
                pv = linels[2*i+1]
                if (pv.find(":")!=-1):
                    print(linels,names[i])
                dictls[i][code]=pv

                
            else:
                ont = 0
                #print(linels)
                for field in range(0,len(linels)):
                    if linels[field]=='':
                        #print(field,ont)
                        ont=ont+1
                    else:
                        try:
                            float(linels[field])
                        except ValueError:
                            #print(field,ont)
                            code = linels[field]
                            pv = linels[field+1]
                            dictls[ont][code]=pv
                            ont = ont+1
                            

    output = []
    for d1 in range(0,len(dictls)):
        dic1 = dictls[d1]
        for d2 in range(0,len(dictls)):
            if (d1!=d2):
                dic2 = dictls[d2]
                ont1=[]
                ont2=[]
                keys=[]
                for key in dic1.keys():
                    if (key in dic2):
                        ont1.append(float(dic1[key]))
                        ont2.append(float(dic2[key]))
                        keys.append(key)
                        
                    elif(key.find("MAP:")!=-1):
                        Map= key.split(':')[1]
                        if("PAM:"+Map in dic2):
                            ont1.append(float(dic1[key]))
                            ont2.append(float(dic2["PAM:"+Map]))
                            keys.append(Map)
                    elif(key.find("PAM:")!=-1):
                        Map= key.split(':')[1]
                        if("MAP:"+Map in dic2):
                            ont1.append(float(dic1[key]))
                            ont2.append(float(dic2["MAP:"+Map]))
                            keys.append(Map)
                            
                tau, p_value = ss.kendalltau(ont1, ont2)
                output.append([names[d1],names[d2],tau,p_value,len(keys)])
    print(ls[strain])
    thefile = open('EAcode/'+ls[strain]+'.txt', 'w')
    #for line in output:
    #    thefile.write("%s\t%s\t%s\n" % (line[0],line[1],line[2]))
    #    print(line)
    thefile.close()

        
        
    
