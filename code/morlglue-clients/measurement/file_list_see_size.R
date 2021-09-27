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


library(pryr)

mem_format <- function(x){
  format(x,digits=3)
}
count_size <-function(source_path){
  file_list <- get_csv_file_list(source_path)
  cache_version_filepath <- paste0(source_path,"get_presummarized_csv_activity_dt_20210902_cache.RData")
  print(cache_version_filepath)
  if(file.exists(cache_version_filepath)){
    load(cache_version_filepath)
    return(output)
  }
  episode_summary_list<-list()
  run_summary_list <- list()
  for (row_i in 1:nrow(file_list)){
    row <- file_list[row_i,]
    cat(".")
    #load the spreadsheet
    csv_data_dt <- readr::read_csv(
      paste0(source_path,row[["filename"]]),show_col_types = FALSE) %>% data.table
    
    print(paste(row[["filename"]], nrow(csv_data_dt),ncol(csv_data_dt)))
  }
  return(output)
}

source_path <- "../data/multirun_n100_eeba_rolf/"

start_time <- Sys.time()

#file_list <- get_csv_file_list(source_path)

#raw_activity <- get_raw_csv_activity_dt(file_list[1:5,],source_path)
raw_activity <- count_size(source_path)

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

