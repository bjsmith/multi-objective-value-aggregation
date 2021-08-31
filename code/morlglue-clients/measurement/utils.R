blackman50_window <- signal::blackman(50)/sum(signal::blackman(50))

blackman50_function<-function(steps){
  return(sum(blackman50_window*steps))
}
blackman200_window <- signal::blackman(200)/sum(signal::blackman(200))
blackman200_function<-function(steps){
  return(sum(blackman200_window*steps))
}


append_blackman_averaging_simple <- function(activity_long){
  
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

get_file_list <- function(source_path){
  #list files
  files <- list.files(source_path,pattern = "*.xls")
  #regex read the main properties of each sheet
  file_list <- stringr::str_match(files,'^([\\w\\d\\.]*)\\(([\\w,]*)\\)-(\\w*)\\(([\\w,]*)\\)') %>% data.frame %>% cbind(files,.)
  colnames(file_list) <- c("filename","full_code","Environment","EnvironmentClass","Agent","AgentClass")
  return(file_list)
}

get_csv_file_list <- function(source_path){
  #list files
  files <- list.files(source_path,pattern = "*.csv")
  #regex read the main properties of each sheet
  file_list <- stringr::str_match(files,'^([\\w\\d\\.]*)\\(([\\w,]*)\\)-(\\w*)\\(([\\w,]*)\\)') %>% data.frame %>% cbind(files,.)
  colnames(file_list) <- c("filename","full_code","Environment","EnvironmentClass","Agent","AgentClass")
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
