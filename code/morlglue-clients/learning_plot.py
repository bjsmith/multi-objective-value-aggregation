import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

ELA_data = pd.read_excel("Sokoban-ELA(ELA)-SOFTMAY_T10-alpha0.1-lambda0.95-dt20210519220402.xls")
LELA_data = pd.read_excel("Sokoban-LELA(LELA)-SOFTMAY_T10-alpha0.1-lambda0.95-dt20210519220408.xls")
MIN_data = pd.read_excel("Sokoban-MIN(MIN)-SOFTMAY_T10-alpha0.1-lambda0.95-dt20210519220359.xls")
SFLLA_data = pd.read_excel("Sokoban-SFLLA(SFLLA)-SOFTMAY_T10-alpha0.1-lambda0.95-dt20210519220404.xls")
SFMLA_data = pd.read_excel("Sokoban-SFMLA(SFMLA)-SOFTMAY_T10-alpha0.1-lambda0.95-dt20210519220407.xls")
Linear_data = pd.read_excel("Sokoban-SFMLA(SFMLA)-SOFTMAY_T10-alpha0.1-lambda0.95-dt20210519220407.xls")
SO_data = pd.read_excel("Sokoban-SingleObjective(SOSE)-SOFTMAY_T10-alpha0.1-lambda0.95-dt20210519224627.xls")
TLOA_data = pd.read_excel("Sokoban-TLO_A(SafetyFirstMO)-SOFTMAY_T10-alpha0.1-lambda0.95-dt20210519224622.xls")

num_online = 5000
num_offline = 1
objectives = ["R^P", "R^A", "R^*"]
agents = {
        "ELA": ELA_data,
        #"LELA": LELA_data,
        "MIN": MIN_data,
        #"SFLLA": SFLLA_data,
        #"SFMLA": SFMLA_data, 
        #"Linear": Linear_data,
        #"SO": SO_data,
        "TLO_A": TLOA_data
        }

#data = [ELA_data, LELA_data, MIN_data, SFLLA_data, SFMLA_data, Linear_data, SO_data, TLOA_data]
dataframes = []

for dat in agents.values():
    dataframes.append(pd.DataFrame(dat, columns=objectives))

fig, ax = plt.subplots(3, 1, figsize=(10, 15))
lines_RP = []
lines_RA = []
lines_RS = []
kernelwidth = 10
for ai, ag_data in enumerate(dataframes):
    runmean_RP = np.convolve(ag_data.iloc[:num_online, [0]].to_numpy()[:,0], np.ones(kernelwidth)/kernelwidth, mode="valid")
    print(runmean_RP)
    line_RP = ax[0].plot(runmean_RP)
    lines_RP.append(*line_RP)
    runmean_RA = np.convolve(ag_data.iloc[:num_online, [1]].to_numpy()[:,0], np.ones(kernelwidth)/kernelwidth, mode="valid")
    line_RA = ax[1].plot(runmean_RA)
    lines_RA.append(*line_RA)
    runmean_RS = np.convolve(ag_data.iloc[:num_online, [2]].to_numpy()[:,0], np.ones(kernelwidth)/kernelwidth, mode="valid")
    line_RS = ax[2].plot(runmean_RS)
    lines_RS.append(*line_RS)

ax[0].set_ylabel("R^P")
ax[0].legend(lines_RP, agents.keys())
ax[1].set_ylabel("R^A")
ax[1].legend(lines_RA, agents.keys())
ax[2].set_ylabel("R^*")
ax[2].legend(lines_RS, agents.keys())
ax[0].grid()
ax[1].grid()
ax[2].grid()
ax[2].set_xlabel("episode")
plt.savefig("Sokoban_bestR*_learningcurves.pdf", bbox_inches="tight")
plt.show()
