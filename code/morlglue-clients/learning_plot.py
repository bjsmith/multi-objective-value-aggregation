print("program runs")
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import argparse
import datetime
import time
import os
from pathlib import Path

# define command line arguments
parser = argparse.ArgumentParser()

parser.add_argument('--name', type=str, default='test')
parser.add_argument('--path', type=str, default='data')
parser.add_argument('--format', type=str, default='csv')
parser.add_argument('--title', type=str, default='')
parser.add_argument('--files', default=["invalidfile"], nargs="+")
parser.add_argument('--objectives', default=["R^P", "R^A", "R^*"], nargs="+")
parser.add_argument('--timestamp', action='store_true')
parser.add_argument('--show', action='store_true')
parser.add_argument('--num_offline', type=int, default=1)
parser.add_argument('--num_online', type=int, default=5000)
parser.add_argument('--kernelwidth', type=int, default=10)

args = parser.parse_args() 

agents = {} # store agents with corresponding data (assumes same environment for all)

# parse given file names
for f in args.files:
    splitlist = f.split("-")
    envname = splitlist[0]
    agentname = splitlist[1]
    if args.format == "xls":
        data = pd.read_excel(f+".xls")
    elif args.format == "csv":
        data = pd.read_csv(f+".csv")
    agents[agentname] = data
    print("added ",agentname)

# get relevant objectives from data
dataframes = []
for dat in agents.values():
    dataframes.append(pd.DataFrame(dat, columns=args.objectives))

# plot learning curves
fig, ax = plt.subplots(len(args.objectives), 1, figsize=(10, 15))

if args.title != '':
    ax[0].set_title(args.title)

lines = [[]]*len(args.objectives) # store line objects for legend display

for io, o in enumerate(args.objectives):
    for ai, ag_data in enumerate(dataframes):
        runmean = np.convolve(ag_data.iloc[:args.num_online, [io]].to_numpy()[:,0], np.ones(args.kernelwidth)/args.kernelwidth, mode="valid")
        line = ax[io].plot(runmean, label=list(agents.keys())[ai])
        lines[io].append(line[0])
    
for io, o in enumerate(args.objectives):
    ax[io].set_ylabel(o)
    ax[io].legend(lines[io], agents.keys())
    ax[io].grid()
    if io == len(args.objectives) - 1:
        ax[io].set_xlabel("episode")

# save and show plot
if args.timestamp:
    ts = time.time()
    st = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d-%H:%M:%S')
    save_path = "{}/{}_learningcurves_{}.pdf".format(args.path, args.name, st)
    print(save_path)
    plt.savefig(save_path, bbox_inches="tight")
else:
    save_path = "{}/{}_learningcurves.pdf".format(args.path, args.name)
    print(save_path)
    plt.savefig(save_path, bbox_inches="tight")
if args.show:
    plt.show()
