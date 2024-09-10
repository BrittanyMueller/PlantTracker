FROM registry.larrycloud.ca/qawse3dr/plant-tracker:latest

RUN mkdir planttracker

COPY ./plant-tracker.jar /planttracker/plant-tracker.jar


WORKDIR /planttracker
ENTRYPOINT [ "bash", "-c", "service postgresql start && java -jar /planttracker/plant-tracker.jar --config /planttracker/secrets/docker_config.json"]


