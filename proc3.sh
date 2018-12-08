docker run -p 4003:4003 \
           -e VIEW="10.0.0.2:4002,10.0.0.3:4003,10.0.0.4:4004" \
           -e IP_PORT="10.0.0.3:4003" \
           --net=mynet --ip=10.0.0.3 \
           -it assignment4
