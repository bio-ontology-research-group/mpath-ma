% Sarah M. Alghamdi

% this code plots the ROC curves for all the mice groups, using the similarities generated by different ontologies

ontologies_paths = { 'similarities/MA/MA','similarities/MAP/MAP','similarities/MAPT/MAP','similarities/PAM/PAM','similarities/PAMT/PAM','similarities/MPATH/MPATH'};
ontology_label  = {'MA','MAP','MAPT','PAM','PAMT','MPATH'};
tested_groups = {'_6M_F_sim.txt','_6M_M_sim.txt','_12M_F_sim.txt','_12M_M_sim.txt','_20M_F_sim.txt','_20M_M_sim.txt','_LONG_F_sim.txt','_LONG_M_sim.txt'};
AUC = zeros(6,8);
for group=1:8
    tab = readtable(char(strcat('similarities/MA/MA',tested_groups(group))));
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
    u = unique(strains);
    length(u)

    m = zeros(s,6);
    %loop to get the purity of clusters from 2 clusters to the number of mice
    %in the selected group


    TPR = zeros(s,6);
    FPR = zeros(s,6);
    % loop through the 6 similarities matrices(each similarity is computed based on one ontology)
    for  ontology = 1:6
        tab = readtable(char(strcat(ontologies_paths(ontology),tested_groups(group))));
        tab = tab(:,2:(width(tab)-1));
        tab = table2array(tab);
        %n is ranks to consider
        for n = 1:s
            % find the n th top ranked mice for each mouse and see if they
            % match the mouse strain 
            ind = grp2idx(strains);
            P = 0;
            TP = 0;
            FP =0;
            N = 0; 
            for mouse = 1:length(strains)
                 [sortedValues,sortIndex] = sort(tab(mouse,:),'descend');
                 N = N+ (s - sum(ind == ind(mouse)));
                 P = P + sum(ind == ind(mouse));
                 for i = 1:n
                     if(ind(mouse)==ind(sortIndex(i)))
                         TP=TP+1;
                     else
                         FP=FP+1;
                     end
                 end
            end
            TPR(n,ontology) = TP/P;
            FPR(n,ontology) = FP/N;
        end


    end   
%%
    for o= 1:6
        AUC(o,group) = trapz(FPR(:,o),TPR(:,o));

    end
%%
    figure
    plot(FPR(:,1),TPR(:,1),FPR(:,2),TPR(:,2),FPR(:,3),TPR(:,3),FPR(:,4),TPR(:,4),FPR(:,5),TPR(:,5),FPR(:,6),TPR(:,6));

    %xlim([0 1])
    xlabel('FPR') % x-axis label
    ylabel('TPR') % y-axis label
    legend('MA','MAP','MAPT','PAM','PAMT','MPATH')  
    fig = gcf;
    fig.PaperPositionMode = 'auto';
    fig_pos = fig.PaperPosition;
    fig.PaperSize = [fig_pos(3) fig_pos(4)];
    saveas(fig,char(strcat(tested_groups(group),'.pdf')))
end