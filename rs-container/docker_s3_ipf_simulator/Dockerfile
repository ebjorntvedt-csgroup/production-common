ARG BRANCH
FROM artifactory.coprs.esa-copernicus.eu/rs-docker/rs-core-execution-worker:${BRANCH} as build
WORKDIR /app

FROM artifactory.coprs.esa-copernicus.eu/rs-docker/rs-core-base:${BRANCH}
ARG VERSION
ARG COMMIT_ID
ARG BRANCH_TEXT 

WORKDIR /app
RUN echo "${VERSION}" >> VERSION
RUN echo "${BRANCH_TEXT}" >> VERSION
RUN echo "${COMMIT_ID}" >> VERSION

USER root:root

RUN mkdir -p /usr/local/conf
#ADD files/ipf-sim-s3-2.5.1_bc1.tar.gz /usr/local/conf
ADD files/IPFSimulator-4.0.13.tgz /usr/local/conf
ADD files/tasktables /usr/local/conf/IPFSimulator/TaskTables
 
#COPY IPFSimulator.tar.gz /usr/local/conf
#RUN tar -xvzf /usr/local/conf/IPFSimulator.tar.gz -C /usr/local/conf

# Patch used java executable in IPF Simulator
#RUN sed -i s,/opt/java1.8/bin/java,java,g /usr/local/conf/IPFSimulator/ipfScripts/Processor_wrapper.sh
#RUN ln -s /usr/local/conf/IPFSimulator/bin/Processor_wrapper.sh /usr/local/conf/IPFSimulator/bin/ipf-sim.sh
#RUN ln -s /usr/local/conf/IPFSimulator/bin/Processor_wrapper.sh S3IPF.sh
RUN ln -s /usr/local/conf/IPFSimulator/bin/ipf-sim.sh S3IPF.sh

# link task table binaries
# rsync required for ipf-sim-for-tc25
RUN apt-get update && apt-get install rsync -y

ADD files/linkBin.sh /app/linkBin.sh
RUN /app/linkBin.sh

COPY --from=build /app/rs-execution-worker.jar rs-execution-worker.jar
ADD files/start.sh /app/start.sh

ADD files/ipf-sim-for-tc25.sh /usr/local/components/S3IPF_L0_05.02/bin/OL_0_CR_.bin

# Create folder for IPFSimulator
RUN mkdir -p /data/ipf-s3

ENTRYPOINT ["/app/start.sh"]
 