blackman50_window <- signal::blackman(50)/sum(signal::blackman(50))

blackman50_function<-function(steps){
  return(sum(blackman50_window*steps))
}
blackman200_window <- signal::blackman(200)/sum(signal::blackman(200))
blackman200_function<-function(steps){
  return(sum(blackman200_window*steps))
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
    return(TLO_A_page0)
  })
  
  raw_activity <- do.call(rbind,raw_activity_list)
  return(raw_activity)
}

