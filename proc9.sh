docker run -p 4009:4009 \
           -e VIEW="10.0.0.2:4002,10.0.0.3:4003,10.0.0.4:4004" \
           -e IP_PORT="10.0.0.9:4009" \
           --net=mynet --ip=10.0.0.9 \
           -it assignment3
