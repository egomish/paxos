docker run -p 4004:4004 \
           -e VIEW="10.0.0.2:4002,10.0.0.3:4003,10.0.0.4:4004" \
           -e IP_PORT="10.0.0.4:4004" \
           --net=mynet --ip=10.0.0.4 \
           -it assignment4
