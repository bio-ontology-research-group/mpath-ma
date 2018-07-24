% Sarah M. Alghamdi

% This code compares between the ranking of two ontologies for pairs of mice who are in the same strains.
% to run this please select the group (line 10)

ontologies_paths = { 'similarities/MA/MA','similarities/MAP/MAP','similarities/MAPT/MAP','similarities/PAM/PAM','similarities/PAMT/PAM','similarities/MPATH/MPATH'};
ontology_label  = {'MA','MAP','MAPT','PAM','PAMT','MPATH'};
tested_group = {'_6M_F_sim.txt','_6M_M_sim.txt','_12M_F_sim.txt','_12M_M_sim.txt','_20M_F_sim.txt','_20M_M_sim.txt','_LONG_F_sim.txt','_LONG_M_sim.txt'};

group = 3;
%ontology1 = 1;
%ontology2 = 2;

tab = readtable(char(strcat('similarities/MA/MA',tested_group(group))));
mice1 = tab(:,1);
mice1 = table2array(mice1);
tab = tab(:,2:(width(tab)-1));

%this is list of all the mice along with thier strains
mice = readtable('mice.csv','Delimiter',',','ReadVariableNames',0);
mice = table2array(mice);

% here the strains of the chosen group are captured
miceI = mice(:,1);
%I = (miceI==mice1);
[C,ia,ib] = intersect(miceI,mice1,'stable');
strains = mice(ia,:);
strains = strains(:,2);
s = length(strains);


ind = grp2idx(strains);
uniq = unique(ind);
u = length(uniq);

% count number of pairs to be used in the wilcoxon test
numberOfpairs = 0;
for n = 1:u
    I = (ind==uniq(n));
    %x = ind(I>0);
    if(sum(I)<1)
        numberOfpairs= numberOfpairs + sum(I) + nchoosek(sum(I),2);
    else
        numberOfpairs= numberOfpairs +1 ;
    end
end     
%%
pv =zeros(6,6);
hv = zeros(6,6);

for  ontology1 = 1:6
    
    tab1 = readtable(char(strcat(ontologies_paths(ontology1),tested_group(group))));
    tab1 = tab1(:,2:(width(tab1)-1));
    tab1 = table2array(tab1);
    
    for  ontology2 = 1:6
    
        tab2 = readtable(char(strcat(ontologies_paths(ontology2),tested_group(group))));
        tab2 = tab2(:,2:(width(tab2)-1));
        tab2 = table2array(tab2);

        %list of ranks by the 2 ontologies to be compared
        % columns in ranks: mouse1, mouse2, similarity based on ontology1,
        % similarity based on ontology2
        ranks = zeros(numberOfpairs,4);
        index = 1;
        for n = 1:s
            for i= n:s
                if(ind(n)==ind(i))
                    
                    ranks(index,1)=n;
                    ranks(index,2)=i;
                    ranks(index,3)=tab1(n,i);
                    ranks(index,4)=tab2(n,i);
                    index = index+1;
                    
                end
            end
        end
        [p,h] = ranksum(ranks(:,3),ranks(:,4));
        pv(ontology1,ontology2)=p;
        hv(ontology1,ontology2)=h;
    end
    min(ranks(:,3))
    max(ranks(:,3))
end
        





