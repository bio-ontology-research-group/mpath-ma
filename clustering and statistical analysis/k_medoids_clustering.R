# Sarah M. Alghamdi

# this code applies k-medoids clustering using PAM algorithm (partitioning around medoids)


most.common <- function(x) {
  count <- sapply(unique(x), function(i) sum(x==i, na.rm=TRUE))
  return(unique(x)[which(count==max(count))][1])
}

ontology_paths = c('MA/MA','MAP/MAP','MAPT/MAP','PAM/PAM','PAMT/PAM','MPATH/MPATH')
group_= '20M_M'

tab = read.table(file=paste("similarities/",ontology_paths[1],"_",group_,"_sim.txt",sep=""), sep="\t")
mice1 = tab[,1]
#this is list of all the mice along with thier strains
mice = read.table('mice.csv',sep=',');
# here the strains of the chosen group are captured
miceI = mice[,1];
ia = which(miceI%in%mice1)
strains = mice[ia,2];
s = length(strains)-1;
m = matrix(0,s,6);
ind<-as.array(factor(strains,labels = 1:length(unique(strains))))

for  (ontology in 1:6)
{
  #getting the Data (similarity matrces)
  tab = read.table(file=paste("similarities/",ontology_paths[ontology],"_",group_,"_sim.txt",sep=""), sep="\t")
  tab = tab[2:nrow(tab),2:(ncol(tab)-1)];
  # converting table to array and getting the dissimilarity 
  # since the clustering needs the dissimilarity not the similarity matrix
  mdist<-as.dist(tab)
  mdist<-mdist*-1

  
  for  (n in 2:s)
  {
    
    #clustering is done here
    mpam = cluster::pam(mdist, n, diss=TRUE);
    
    #To compute purity , each cluster is assigned to the class which is most frequent in the cluster, and then the accuracy of this assignment is measured by counting the number of correctly assigned documents and dividing by $N$
    #ind = grp2idx(strains);
    
    newC=matrix(0,length(mpam$clustering),1)
    for (i in 1:length(mpam$clustering))
    {
      I = (mpam$clustering==mpam$clustering[i]);
      x = ind[I>0];
      newC[i] = most.common(x);
    }
    
    newC<-array(newC)
    I = (newC==ind)
    purity = (length(newC[I>0])/length(ind))*100;
    m[n,ontology] = purity;
  
  }
  
  
}
write.csv(m, file = paste(group_,".csv",sep=''))
