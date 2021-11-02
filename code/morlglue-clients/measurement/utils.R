

use_display_names <- function(table){
  if("Agent" %in% colnames(table)){
    #https://stackoverflow.com/questions/44310088/how-to-add-latex-code-in-ggplot2-legend-labels
    levels(table$Agent)[levels(table$Agent)=="SFLLA"] <- "SFELLA"
    levels(table$Agent)[levels(table$Agent)=="SFLLA2"] <- "SFELLA_rt"
    levels(table$Agent)[levels(table$Agent)=="SEBA2"] <- "SEBA_rt"
    levels(table$Agent)[levels(table$Agent)=="TLO_A"] <- "TLO^A"
    levels(table$Agent)[levels(table$Agent)=="LIN_SUM"] <- "LinearSum"
  }
  return(table)
}


get_blackman_smoothing_function <- function(smoothing_level){
  blackman_x_window <- signal::blackman(smoothing_level)/sum(signal::blackman(smoothing_level))
  smoothing_function <- function(steps){
    return(sum(blackman_x_window*steps))
  }
  return(smoothing_function)
}

blackman10_function<-get_blackman_smoothing_function(10)
blackman20_function<-get_blackman_smoothing_function(20)
blackman50_function<-get_blackman_smoothing_function(50)
blackman200_function<-get_blackman_smoothing_function(200)


append_blackman_averaging_dt <- function(activity_long){
  
  activity_long_out <- activity_long %>% 
    mutate(
      ScoreRMean10 = frollmean(Score,20),
      ScoreBlackman = frollapply(Score,50,blackman50_function),
      ScoreBlackman200 = frollapply(Score,200,blackman200_function)
    ) %>% ungroup %>% data.table
  return(activity_long_out)
}



append_blackman_averaging <- function(activity_long){
  
  activity_long_out <- activity_long %>% group_by(Measure,EpisodeType,Agent,Environment) %>% 
    mutate(
      ScoreRMean10 = frollmean(Score,20),
      ScoreBlackman = frollapply(Score,50,blackman50_function),
      ScoreBlackman200 = frollapply(Score,200,blackman200_function)
    ) %>% ungroup %>% data.table
  return(activity_long_out)
}

#this string does not handle the granularity features
#filename_regex_parse_string <- "^([\\w\\d\\.]*)\\(([\\w,]*)\\)-(\\w*)\\(([\\w,]*)\\)"
#this string can handle granularity features
filename_regex_parse_string <- "^([\\w\\d\\.]*)\\(([\\w,]*)\\)-(\\w*)(rew_gran([\\w\\d\\.\\_]*)pen_gran([\\w\\d\\.\\_]*))?\\(([\\w,]*)\\)"
get_file_list <- function(source_path){
  #list files
  files <- list.files(source_path,pattern = "*.xls")
  #regex read the main properties of each sheet
  file_list <- stringr::str_match(files,filename_regex_parse_string) %>% data.frame %>% cbind(files,.)
  colnames(file_list) <- c("filename","full_code","Environment","EnvironmentClass","Agent","AgentClass")
  return(file_list)
}

get_csv_file_list <- function(source_path){
  #list files
  files <- list.files(source_path,pattern = "*\\.csv$")
  #regex read the main properties of each sheet
  regex_matches <- stringr::str_match(files,filename_regex_parse_string)
  file_list <- regex_matches %>% data.frame %>% cbind(files,.)
  if(ncol(regex_matches)==8){
    colnames(file_list) <- c("filename","full_code","Environment","EnvironmentClass","Agent","GranularityCode","RewGranularity","PenGranularity", "AgentClass")
  }else if (ncol(regex_matches)==6){
    colnames(file_list) <- c("filename","full_code","Environment","EnvironmentClass","Agent", "AgentClass")
  }else{ 
    stop(paste0("Unrecognized file name metadata format for set of files in ", source_path))
  }
  
  return(file_list)
}

get_raw_activity <-function(file_list,source_path){
  #now we want to iterate through each of those and output the data
  raw_activity_list <- apply(file_list,1,function(row){
    #print(row[["full_code"]]) 
    #load the spreadsheet
    TLO_A_page0 <- readxl::read_xls(
      paste0(source_path,row[["filename"]]),
      sheet = "Trial0")
    
    #clean the data
    colnames(TLO_A_page0)[1] <- "EpisodeType"
    TLO_A_page0$`Episode number` <- as.numeric(TLO_A_page0$`Episode number`)
    #label the data
    TLO_A_page0$Agent <- row[["Agent"]]
    TLO_A_page0$Environment <- row[["Environment"]]
    TLO_A_page0$EnvironmentClass <- row[["EnvironmentClass"]]
    TLO_A_page0$Filename = row[["filename"]]
    return(TLO_A_page0)
  })
  
  raw_activity <- do.call(rbind,raw_activity_list)
  raw_activity$FileID <- as.numeric(as.factor(raw_activity$Filename))
  raw_activity$Filename <- NULL #we don't really need this, we just need to store a unique ID for each file.
  return(raw_activity)
}


get_raw_csv_activity <-function(file_list,source_path){
  #now we want to iterate through each of those and output the data
  raw_activity_list <- apply(file_list,1,function(row){
    #print(row[["full_code"]]) 
    #load the spreadsheet
    TLO_A_page0 <- readr::read_csv(
      paste0(source_path,row[["filename"]]))
    
    #clean the data
    colnames(TLO_A_page0)[1] <- "EpisodeType"
    TLO_A_page0$`Episode number` <- as.numeric(TLO_A_page0$`Episode number`)
    #label the data
    TLO_A_page0$Agent <- row[["Agent"]]
    TLO_A_page0$Environment <- row[["Environment"]]
    TLO_A_page0$EnvironmentClass <- row[["EnvironmentClass"]]
    TLO_A_page0$Filename = row[["filename"]]
    
    #add an trial iteration label if it's not there already.
    #print(TLO_A_page0 %>% group_by(EpisodeType) %>% summarise(MaxEpisode=max(`Episode number`)))
    trial_iteration_by_episode_type <- TLO_A_page0 %>% group_by(EpisodeType) %>% summarise(MaxEpisode=max(`Episode number`))
    episodes_per_trial_iteration <- sum(trial_iteration_by_episode_type$MaxEpisode)
    #print(episodes_per_trial_iteration)
    #now get the number of trials; should be the repetition of each episode number
    #which should be the same for all episode numbers! We can do it just for episode 1 to speed things up on the assumption
    #that all episodes present are run for the same number of times
    trial_count <- TLO_A_page0 %>% filter(`Episode number`==1 & EpisodeType=="Online") %>% nrow
    
    TLO_A_page0$RunId <- rep(1:trial_count,each=episodes_per_trial_iteration)
    
    return(TLO_A_page0)
  })
  
  raw_activity <- do.call(rbind,raw_activity_list)
  raw_activity$FileID <- as.numeric(as.factor(raw_activity$Filename))
  raw_activity$Filename <- NULL #we don't really need this, we just need to store a unique ID for each file.
  return(raw_activity)
}



# get_presummarized_csv_activity <-function(source_path){
#   file_list <- get_csv_file_list(source_path)
#   cache_version_filepath <- paste0(source_path,"get_presummarized_csv_activity_dt_20210902_cache.RData")
#   print(cache_version_filepath)
#   if(file.exists(cache_version_filepath)){
#     load(cache_version_filepath)
#     return(output)
#   }
#   
#   
#   #now we want to iterate through each of those and output the data
#   raw_activity_list <- apply(file_list,1,function(row){
#     #print(row[["full_code"]])
#     cat(".")
#     
#     csv_data <- readr::read_csv(
#       paste0(source_path,row[["filename"]]))
#     
#     #clean the data
#     colnames(csv_data)[1] <- "EpisodeType"
#     csv_data$`Episode number` <- as.numeric(csv_data$`Episode number`)
#     #label the data
#     csv_data$Agent <- row[["Agent"]]
#     csv_data$Environment <- row[["Environment"]]
#     csv_data$EnvironmentClass <- row[["EnvironmentClass"]]
#     csv_data$Filename = row[["filename"]]
# 
#     #add an trial iteration label if it's not there already.
#     #print(TLO_A_page0 %>% group_by(EpisodeType) %>% summarise(MaxEpisode=max(`Episode number`)))
#     trial_iteration_by_episode_type <- csv_data %>% group_by(EpisodeType) %>% summarise(MaxEpisode=max(`Episode number`)) %>% ungroup()
#     episodes_per_trial_iteration <- sum(trial_iteration_by_episode_type$MaxEpisode)
#     #print(episodes_per_trial_iteration)
#     #now get the number of trials; should be the repetition of each episode number
#     #which should be the same for all episode numbers! We can do it just for episode 1 to speed things up on the assumption
#     #that all episodes present are run for the same number of times
#     trial_count <- csv_data %>% filter(`Episode number`==1 & EpisodeType=="Online") %>% nrow
#     
#     csv_data$RunId <- rep(1:trial_count,each=episodes_per_trial_iteration)
#     
#     
#     csv_data_dt_episode_summary <- csv_data %>% data.table %>% csv_data_dt[
#       ,.(
#         `R^P`=mean(`R^P`),
#         `R^A`=mean(`R^A`),
#         `R^*`=mean(`R^*`)
#       )
#       ,.(EpisodeType,`Episode number`,Agent,Environment,EnvironmentClass)
#     ]
#     
#     csv_data_dt_run_summary <- csv_data %>% data.table %>% csv_data_dt[
#       
#       ,.(
#         `R^P`=mean(`R^P`),
#         `R^A`=mean(`R^A`),
#         `R^*`=mean(`R^*`)
#       )
#       ,.(EpisodeType,RunId,Agent,Environment,EnvironmentClass,RunId)
#     ]
#     rm(csv_data)
#     
#     return(list("episode_summary"=csv_data_dt_episode_summary,"run_summary"=csv_data_dt_run_summary))
#   })
#   
#   
#   episode_summary_list <- lapply(raw_activity_list,function(li){return(li[["episode_summary"]])})
#   run_summary_list <- lapply(raw_activity_list,function(li){return(li[["run_summary"]])})
#   
#   episode_summary <- data.table::rbindlist(episode_summary_list)
#   run_summary <- data.table::rbindlist(run_summary_list)
#   output<-list("episode_summary"=episode_summary,"run_summary"=run_summary)
#   save(output,file=cache_version_filepath)
#   return(output)
# }


get_presummarized_csv_activity_dt <-function(source_path){
  start_time <- Sys.time()
  file_list <- get_csv_file_list(source_path)
  cache_version_filepath <- paste0(source_path,"get_presummarized_csv_activity_dt_20210902_cache.RData")
  print(cache_version_filepath)
  if(file.exists(cache_version_filepath)){
    load(cache_version_filepath)
    return(output)
  }
  episode_summary_list<-list()
  run_summary_list <- list()
  print(file_list)
  file_list_length <-nrow(file_list)
  next_update_time <- start_time+10 #update for output
  for (row_i in 1:file_list_length){
    
    row <- file_list[row_i,]
    
    current_time <- Sys.time()
    if(current_time > next_update_time){
      next_update_time <- current_time+10
      progress_pct <- round((row_i-1)/file_list_length * 100,0)
      cat(paste0("Progress: ", as.character(progress_pct), "%\r"))
      flush.console()      
    }

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
    
    condition_cols <-c("EpisodeType","Agent","Environment","EnvironmentClass")
    
    csv_data_dt_episode_summary <- csv_data_dt[
      
      ,.(
        `R^P`=mean(`R^P`),
        `R^A`=mean(`R^A`),
        `R^*`=mean(`R^*`)
      )
      ,c("Episode number",condition_cols)
    ]
    
    csv_data_dt_run_summary <- csv_data_dt[
      
      ,.(
        `R^P`=mean(`R^P`),
        `R^A`=mean(`R^A`),
        `R^*`=mean(`R^*`)
      )
      ,c("RunId",condition_cols)
    ]
    rm(csv_data_dt)
    
    if(("RewGranularity" %in% names(row)) & ("PenGranularity"%in% names(row))){
      #it's inconsistent to add these down here and the other summary vars above, but adding them above is just legacy code I haven't changed.
      #BJS 2021-10-18
      csv_data_dt_episode_summary[,RewGranularity:=as.numeric(row[["RewGranularity"]])]
      csv_data_dt_episode_summary[,PenGranularity:=as.numeric(row[["PenGranularity"]])]
      csv_data_dt_run_summary[,RewGranularity:=as.numeric(row[["RewGranularity"]])]
      csv_data_dt_run_summary[,PenGranularity:=as.numeric(row[["PenGranularity"]])]
    }
    
    
    episode_summary_list <- append(episode_summary_list,list(csv_data_dt_episode_summary))
    run_summary_list <- append(run_summary_list,list(csv_data_dt_run_summary))
    
    #print(length(episode_summary_list))
    #print(length(run_summary_list))
    gc()
    
  }
  print("finished getting the lists. concatenating...")
  episode_summary <- rbindlist(episode_summary_list)
  run_summary <- rbindlist(run_summary_list)
  
  
  output<-list("episode_summary"=episode_summary,"run_summary"=run_summary)
  save(output,file=cache_version_filepath)
  return(output)
}
