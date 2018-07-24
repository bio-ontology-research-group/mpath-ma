% Sarah M. Alghamdi

% this code uses similarity matrces to cluster the mice

% getting the Data (similarity matrces)
ontologies_paths = { 'similarities/MA/MA','similarities/MAP/MAP','similarities/MAPT/MAP','similarities/PAM/PAM','similarities/PAMT/PAM','similarities/MPATH/MPATH'};
tested_group = '_20M_F_sim.txt';  % 12M_M , 12M_F, 20M_M, 20M_F, LONG_M or LONG_F
cluster_method = 'NJ'; %CL, NJ or UPGMA
tab = readtable(strcat('similarities/MA/MA',tested_group));
mice1 = tab(:,1);
mice1 = table2array(mice1);
tab = tab(:,2:(width(tab)-1));

% this is list of all the mice along with thier strains
mice = readtable('mice.csv','Delimiter',',','ReadVariableNames',0);
mice = table2array(mice);

% here the strains of the chosen group are captured
miceI = mice(:,1);
[C,ia,ib] = intersect(miceI,mice1,'stable');
strains = mice(ia,:);
strains = strains(:,2);
s = length(strains);
u = unique(strains);
length(u)

m = zeros(s,6);
%loop to get the purity of clusters from 2 clusters to the number of mice
%in the selected group

% loop through the 6 similarities matrices(each similarity is computed based on one ontology)
  for  ontology = 1:6
    
    tab = readtable(char(strcat(ontologies_paths(ontology),tested_group)));
    tab = tab(:,2:(width(tab)-1));
    
    %n is number of clusters
    for n = 2:s
        
        % converting table to array and getting the dissimilarity 
        N = (table2array(tab))*-1; % since the clustering needs the dissimilarity not the similarity matrix
        N = N-min(min(N));
        N = N - diag(diag(N)); % since the squareform only takes 0 diagonal symitric matrix

        %(this form have no dublicated values)
        disim = squareform(N);

        %clustering is done here (prof asked for agglomarative clustering (other linkage could be done))
        if(strcmp(cluster_method,'CL'))
            T = linkage(disim,'complete');
            c = cluster(T,n);
        end
        
        if(strcmp(cluster_method, 'NJ'))
            c1 = seqneighjoin(disim,'equivar',mice1);
            c = cluster(c1,[],'MAXCLUST',n);
        end
        
        if(strcmp(cluster_method,'UPGMA'))
            T = linkage(disim,'average');
            c = cluster(T,n);
        end
        
        
        %To compute purity , each cluster is assigned to the class which is most frequent in the cluster, and then the accuracy of this assignment is measured by counting the number of correctly assigned documents and dividing by $N$
        ind = grp2idx(strains);

        newC=zeros(length(c),1);
        for i=1:length(c)
             I = (c==c(i));
             x = ind(I>0);
             newC(i) = mode(x);
        end

        purity = (sum(newC==ind)/numel(ind));
        m(n,ontology) = purity;
    end
    %%%%%%
    
  end

%% calculate AUC
AUC = [0 0 0 0 0 0];  
AUC(1)= trapz(m(:,1))/length(ind);
AUC(2)= trapz(m(:,2))/length(ind);
AUC(3)= trapz(m(:,3))/length(ind);
AUC(4)= trapz(m(:,4))/length(ind);
AUC(5)= trapz(m(:,5))/length(ind);
AUC(6)= trapz(m(:,6))/length(ind);

AUC