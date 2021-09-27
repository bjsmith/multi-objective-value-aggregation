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
get_presummarized_csv_activity_dt <-function(source_path){
  file_list <- get_csv_file_list(source_path)
  cache_version_filepath <- paste0(source_path,"get_presummarized_csv_activity_dt_20210902_cache.RData")
  print(cache_version_filepath)
  if(file.exists(cache_version_filepath)){
    load(cache_version_filepath)
    return(output)
  }
  episode_summary<-NULL
  run_summary <- NULL
  for (row_i in 1:nrow(file_list)){
    row <- file_list[row_i,]
    cat(".")
    #load the spreadsheet
    csv_data_dt <- readr::read_csv(
      paste0(source_path,row[["filename"]]),show_col_types = FALSE) %>% data.table
    
    #clean the data
    colnames(csv_data_dt)[1] <- "EpisodeType"
    csv_data_dt[,`Episode number`:=as.numeric(csv_data_dt$`Episode number`)]
    #label the data
    csv_data_dt[,Agent:=row[["Agent"]]]
    csv_data_dt[,Environment:=row[["Environment"]]]
    csv_data_dt[,EnvironmentClass:=row[["EnvironmentClass"]]]
    #csv_data_dt[,Filename:=row[["filename"]]]
    
    #add an trial iteration label if it's not there already.
    trial_iteration_by_episode_type <- csv_data_dt[,.(MaxEpisode=max(`Episode number`)),EpisodeType]
    episodes_per_trial_iteration <- sum(trial_iteration_by_episode_type$MaxEpisode)
    #now get the number of trials; should be the repetition of each episode number
    #which should be the same for all episode numbers! We can do it just for episode 1 to speed things up on the assumption
    #that all episodes present are run for the same number of times
    trial_count <- csv_data_dt[`Episode number`==1 & EpisodeType=="Online",] %>% nrow
    #print(trial_count)
    
    
    csv_data_dt[,RunId:=rep(1:trial_count,each=episodes_per_trial_iteration)]
    
    
    csv_data_dt_episode_summary <- csv_data_dt[
      
      ,.(
        `R^P`=mean(`R^P`),
        `R^A`=mean(`R^A`),
        `R^*`=mean(`R^*`)
      )
      ,.(EpisodeType,`Episode number`,Agent,Environment,EnvironmentClass)
    ]
    
    csv_data_dt_run_summary <- csv_data_dt[
      
      ,.(
        `R^P`=mean(`R^P`),
        `R^A`=mean(`R^A`),
        `R^*`=mean(`R^*`)
      )
      ,.(EpisodeType,RunId,Agent,Environment,EnvironmentClass,RunId)
    ]
    rm(csv_data_dt)
    
    if(is.null(episode_summary)){
      episode_summary<-csv_data_dt_episode_summary
    }else{
      episode_summary <- rbind(episode_summary,csv_data_dt_episode_summary)
    }
    if(is.null(run_summary)){
      run_summary<-csv_data_dt_run_summary
    }else{
      run_summary <- rbind(run_summary,csv_data_dt_run_summary)
    }
    if(row_i%%10==0){
      
      print(paste0(mem_format(pryr::mem_used()),
                   "; should be made up of episode_summary (",
                   mem_format(object_size(episode_summary)),") and run_summary (", 
                   mem_format(object_size(run_summary)),")"))
      gc()
      print(paste0(mem_format(pryr::mem_used())))
    }
  }
  
  print("finished summary")
  
  
  output<-list("episode_summary"=episode_summary,"run_summary"=run_summary)
  save(output,file=cache_version_filepath)
  return(output)
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

