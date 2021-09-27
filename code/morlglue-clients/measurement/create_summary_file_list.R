library(readxl)
library(data.table)
library(ggplot2)
library(signal)

library(dplyr)
library(stringr)
library(directlabels)

library(knitr)
library(kableExtra)

source("utils.R")
output_dir <- "../../../output/"


library(pryr)

mem_format <- function(x){
  format(x,digits=3)
}



source_path <- "../data/multirun_n100_eeba_rolf/"

start_time <- Sys.time()

#file_list <- get_csv_file_list(source_path)

#raw_activity <- get_raw_csv_activity_dt(file_list[1:5,],source_path)
raw_activity <- get_presummarized_csv_activity_dt(source_path)

episode_summary<-raw_activity$episode_summary
run_summary<-raw_activity$run_summary
print(object.size(raw_activity))

#get memory efficiencies
# raw_activity$EpisodeType <- as.factor(raw_activity$EpisodeType)
# raw_activity$Environment <- as.factor(raw_activity$Environment)
# raw_activity$Agent <- as.factor(raw_activity$Agent)
# 
# #it's pretty big; this will make operations more efficient.
# raw_activity <- data.table(raw_activity)


end_time <- Sys.time()
duration <- end_time - start_time
print(duration)


file_list <- get_csv_file_list(source_path)[1:10,]

csv_data_dt <- readr::read_csv(
  paste0(source_path,file_list[1,"filename"])) %>% data.table

