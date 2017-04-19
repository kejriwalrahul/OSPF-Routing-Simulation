# OSPF-Routing-Simulation
An OSPF Routing Simulation Program

## Usage

1. Build binaries using ```make```

2. Change into bin directory using ```cd bin```

3. Run the simulator using ```java simulator [options]```
    
    where options can be:

        -i <node_identifier> [default=1]

        -f <input_file>      [default="ipfile"]

        -o <output_file>     [default="outfile-<node_indentifier>.txt"]

        -h <HELLO_INTERVAL>  [default=1]

        -a <LSA_INTERVAL>    [default=5]

        -s <SPF_INTERVAL>    [default=20]

        -d                   (enables debug mode)