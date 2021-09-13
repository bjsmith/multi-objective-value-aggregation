library(ggplot2)
set.seed(42)
category_1_assignment<-rep(1:5,each=1000/5)
category_2_assignment<-rep(1:4,1000/4)
x1 <- rnorm(1000,0,0.5)+category_1_assignment+category_2_assignment
labels1 <- as.character(category_1_assignment*10)
labels2 <- paste("group",category_2_assignment)

ggplot(data.frame(x1,labels1,labels2),
       aes(y=y1,x=labels1,color=labels2,fill=labels2,
           group=interaction(labels1,labels2))
       )+geom_violin()+
  labs(x="treatment_amount")


ggplot(data.frame(x1,labels1,labels2),
       aes(y=y1,x=labels1,color=labels2,fill=labels2,
           group=interaction(labels1,labels2))
)+geom_violin(position="stack",alpha=0.4)+
  labs(x="treatment_amount")

ggplot(data.frame(x1,labels1,labels2),
       aes(y=y1,x=labels1,color=labels2,fill=labels2,
           group=interaction(labels1,labels2))
)+geom_violin(position="dodge",alpha=0.4)+
  labs(x="treatment_amount")

ggplot(data.frame(x1,labels1,labels2),
       aes(y=y1,x=labels1,color=labels2,fill=labels2,
           group=interaction(labels1,labels2))
)+geom_violin(position="dodge2",alpha=0.4)+
  labs(x="treatment_amount")

ggplot(data.frame(x1,labels1,labels2),
       aes(y=y1,x=labels1,color=labels2,fill=labels2,
           group=interaction(labels1,labels2))
)+geom_violin(position="identity",alpha=0.4)+
  labs(x="treatment_amount")


ggplot(data.frame(x1,labels1,labels2),
       aes(y=y1,x=labels1,color=labels2,fill=labels2,
           group=interaction(labels1,labels2))
)+geom_boxplot(alpha=0.4,position="stack")+
  labs(x="treatment_amount")

