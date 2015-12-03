# Operating-Systems-Resource-Manager

By Jason Yao [Java] [C]

## Description

A comparison and simulation of an opportunistic resource manager versus Dijkstra's Banker's algorithm resource manager.

## [Java] Compilation & Running

### Compiling

`javac src/* out/`

### Executing the program

`java out/SimulateResourceManagers <flags> <input_file>`

e.g. `java out/SimulateResourceManagers --verbose testing/input/input-01`

## [C] Compilation & Running

### Compiling
`gcc resource-manager.c -o resource-manager`

### Executing the program

`./resource-manager <path_to_input_file>`

e.g.

`./resource-manager testing/input/input-01`

## Background

Resource management can sometimes be a pain to deal with. In real life usages, the ability to *efficiently* allocate and manage resources is an extremely important 
skillset to have. This program is to show two approaches to resource management- an opportunistic (naive) management system, versus Dijkstra's Banker's Algorithm.

### An Opportunistic Project Manager

An Opportunistic Project Manager (OPM) will satisfy a request if possible, and if not, makes the task wait. When a release of a resource occurs, the OPM will attempt to
satisfy the request in a First-Come-First-Serve(FCFS) manner.

### Dijkstra's Banker's Algorithm



## Licensing

The license for this repo follows the GNU GPL v2, available [here](LICENSE).
