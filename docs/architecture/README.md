### Copernicus Reference System
# COPRS-PRO-ADD RS Processing Architecture and Design Document
### Reference System version V2.0
---
:arrow_heading_up: Go back to the [Production Common Documentation Overview](../README.md) :arrow_heading_up:

## Summary

This document aims to provide the software architecture in cloud environment of Processing sub-system of Copernicus Reference System.

## Table Of Contents
1. [Introduction](#1-introduction)
2. [Software Design Overview](#2-software-design-overview)
3. [Software Architecture Design](#3-software-architecture-design)
4. [Software Component Design](#4-software-component-design)

## 1 Introduction

This document aims to provide the software architecture in cloud
environment of **Copernicus Processing Reference System (COPRS)**
project.

### 1.1 Purpose and Objective

The Reference System provides  platform for production and distribution of  of Sentinel-1, Sentinel2 and Sentinel-3 data, serving as key functional of  Copernicus Space Component (CSC) Ground Segment.

The Processing (PRO) subsystem of the Copernicus Reference System is responsible for core features of processing chain Level0 ,Level1 and Level2 for Sentinel1/2/3 satellites. 

This document aims to describe the software architecture used for the processing sub-system of COPRS project.

### 1.2  Applicable and Reference  Documents
Applicable and Referenced documents are the documents which have reference or relation to the inputs to the writing of this document.

| **Document ID** | **Document Name** |
|---------|--|
|  COPRS-PRO-IM    | Installation Manual |
|  COPRS-PRO-SVD    | Software Verification and Validation Test |


## 2 Software Design Overview

2.1 [Design Standard and Convention](#design-standard-and-convention)
2.2 [Software Static Architecture](#software-static-architecture)
2.3 [Interfaces Context](#interfaces-context)
2.4 [Long Lifetime Software](#long-lifetime-software)
***
The following sections give an overview of the architecture and strategies that will be used for the COPRS 1.1, on trying to justify the decisions that had been made.

Currently the COPRS system is used operationally in the version 1.0 that is based on the S1PRO software that had been orginally developed in order to run the Ground Segment for Sentinel-1.

That system is being operated on a set of micro services that are connected with Kafka with each other. However the workflows in that system are rather static and needs configuration files to be adjusted in order to modify the existing workflows or changing the behaviour of the microservices.

The goal is to transform the existing software in such a way that it can be used together with SCDF. This will have an impact on the way how the software will be deployed to the cluster as well as the configuration will be set.

### Design Standard and Convention
In order to give a better overview of the general system and focus on the important aspects, most diagrams shown will be an abstraction of the system behind. Thus most of the diagrams will show that the microservices are communicating with each other.

However normally between all microservices there will be kafka topics that are storing the events and handling the communication between the different services. Just in case the kafka system will be important, it will be shown. Additionally the diagrams will in general also not provide information about different timeliness that are existing within the system.

### Software Static Architecture

The RS provides the processing chain in a Cloud environment by containerizing the micro-services enabling the systematic processing of the Sentinel satellites data. For Sentinel-1, Sentinel-2 and Sentinel-3 satellits the raw data received by CADIP and EDIP is systematically processed and made available to end consumers via PRIP.

The RS implements CADIP (CADU Interface Delivery Point) interface to ingest the downlinked raw data. The interface allows to query sessions and associated files (chunks) using OData4 interface over the internet. The RS Service detects and downloads the raw data using its Trigger and Worker components. The RS also implements EDIP ("EDRS-ICD-ADSO-1000830938) interfaces that are used by PRO to poll for new CADU/DSIB files and download and ingest raw files. The XBIP interface is the predecessor of the CADIP and realized on the WebDav protocol, but should not be used in new implementations anymore.



### Interfaces Context
The following context diagram illustrates the relationship between the RS PRO system with other systems.

![8b153b50a63b1605974e1d2b59a6c1e9.png](media/8b153b50a63b1605974e1d2b59a6c1e9.png)


### Long Lifetime Software

All RSPRO components are based on widely used and well supported COTS and standard protocols which allow a long lifetime support:

- **Programming language**
	- Java
	- Spring Cloud Data Flow (SCDF)
- **Database management system**
	- MongoDB
	- Elasticsearch
- **Messaging System**
	- Kafka
- **Operating system**
	-	 Linux OS, 
	-	 Kubernetes and Docker based
- **Deployoment**
	-  Ansible
	-  Helm
- **Storage system**
	- Object Store provided by Cloud provider,(Amazon S3 API)
	- Ceph Rook

## 3 Software Architecture Design
3.1 [Overall Architecture](#overall-architecture)
3.2 [Software Data Model](#software-data-model)
3.3 [Software Trade-offs](#software-trade-offs)
***
### Overall Architecture
The diagram below depicts architectural overview of RS Processing sub-system
![c0735b37de1563c86e3c8970eca2ff93.png](media/add_overall_architecture.png)

The RS PRO system is composed structurally by software modules which are split in three different type of components (or microservices):

Trigger
- translate Event in Job based on rules
- could have REST interface to e.g. create on-demand Job
- Scaling-needs: low

Worker
- execute Job à result in new Event or second-step-job
- Scaling-needs: high
- Jobs have status (running, failed, restart, …) but Events not

Frontend components
- Provide REST interfaces for access to persistence (e.g. queries)
- Scaling-needs: low

## Software Data Model

#### Product categories and families

The RS PDGS products and files are grouped into families, themselves
grouped into categories.

<table>
<colgroup>
<col style="width: 26%" />
<col style="width: 22%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Category</strong></th>
<th><strong>Family</strong></th>
<th><strong>Files and products</strong></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td>AUXILIARY_FILES</td>
<td>AUXILIARY_FILE</td>
<td>auxiliary files needed for level processing (AUX and MPL files)</td>
</tr>
<tr class="even">
<td>EDRS_SESSIONS</td>
<td>EDRS_SESSION</td>
<td>RAW and XML files of sessions</td>
</tr>
<tr class="odd">
<td>PLANS_AND_REPORTS</td>
<td>PLAN_AND_REPORT</td>
<td>Plans and reports for S1MIS</td>
</tr>
<tr class="even">
<td rowspan="4">LEVEL_JOBS</td>
<td>L0_JOB</td>
<td>Jobs of the level 0</td>
</tr>
<tr class="odd">
<td>L1_JOB</td>
<td>Jobs of the level 1</td>
</tr>
<tr class="even">
<td>L2_JOB</td>
<td>Jobs of the level 2</td>
</tr>
<tr class="odd">
<td>L0_SEGMENT_JOB</td>
<td>Jobs of the level 0 segment</td>
</tr>
<tr class="even">
<td rowspan="7">LEVEL_PRODUCTS</td>
<td>L0_SLICE</td>
<td>Slices of the level 0</td>
</tr>
<tr class="odd">
<td>L0_ACN</td>
<td>ACNs of the level 0</td>
</tr>
<tr class="even">
<td>L0_BLANK</td>
<td>Slices of level 0 which do not handle a L1 processing (GP and HK for example)</td>
</tr>
<tr class="odd">
<td>L1_SLICE</td>
<td>Slices of the level 1</td>
</tr>
<tr class="even">
<td>L2_SLICE</td>
<td>Slices of the level 2</td>
</tr>
<tr class="odd">
<td>L1_ACN</td>
<td>ACNs of the level 1</td>
</tr>
<tr class="even">
<td>L2_ACN</td>
<td>ACNs of the level 2</td>
</tr>
<tr class="odd">
<td>LEVEL_SEGMENTS</td>
<td>L0_SEGMENT</td>
<td>Segment of level 0</td>
</tr>
<tr class="even">
<td rowspan="4">LEVEL_REPORTS</td>
<td>L0_REPORT</td>
<td>reports of the level 0</td>
</tr>
<tr class="odd">
<td>L1_REPORT</td>
<td>reports of the level 1</td>
</tr>
<tr class="even">
<td>L2_REPORT</td>
<td>reports of the level 2</td>
</tr>
<tr class="odd">
<td>L0_SEGMENT_REPORT</td>
<td>reports of the level 0 segment</td>
</tr>
<tr class="even">
<td rowspan="12">COMPRESSED_PRODUCTS</td>
<td>AUXILIARY_FILE_ZIP</td>
<td>compressed auxiliary files</td>
</tr>
<tr class="odd">
<td>L0_ACN_ZIP</td>
<td>compressed ACNs of the level 0</td>
</tr>
<tr class="odd">
<td>L0_SEGMENT_ZIP</td>
<td>compressed segment of level 0</td>
</tr>
<tr class="even">
<td>L0_SLICE_ZIP</td>
<td>compressed slices of the level 0</td>
</tr>
<tr class="odd">
<td>L1_ACN_ZIP</td>
<td>compressed ACNs of the level 1</td>
</tr>
<tr class="even">
<td>L1_SLICE_ZIP</td>
<td>compressed slices of the level 1</td>
</tr>
<tr class="odd">
<td>L2_ACN_ZIP</td>
<td>compressed ACNs of the level 2</td>
</tr>
<tr class="even">
<td>L2_SLICE_ZIP</td>
<td>Compressed slices of the level 2</td>
</tr>
<tr class="even">
<td>JOB_ORDER</td>
<td>Used for creating the file JobOrder.xml<br />
Not directly put in a message queue</td>
</tr>
</tbody>
</table>

#### Message format per topic

<table style="width:100%;">
<colgroup>
<col style="width: 15%" />
<col style="width: 19%" />
<col style="width: 58%" />
<col style="width: 6%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Product Category</strong></th>
<th><strong>Message format</strong></th>
<th><strong>Exemple</strong></th>
<th><strong>Topic example</strong></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td>AUXILIARY_FILES</td>
<td>{<br />
'productName': "{product name}",<br />
'keyObjectStorage': "{key in the object storage of the file used to extract metadata}"<br />
}</td>
<td>{<br />
"productName": "S1A_OPER_MPL_ORBSCT_20140507T150704 _99999999T999999_0020.EOF",<br />
"keyObjectStorage": "S1A_OPER_MPL_ORBSCT_20140507T150704 _99999999T999999_0020.EOF"<br />
}<br />
{<br />
"productName": "S1A_AUX_PP1_V20171017T080000 _G20171013T101236.SAFE",<br />
"keyObjectStorage": "S1A_AUX_PP1_V20171017T080000 _G20171013T101236.SAFE/manifest.safe"<br />
}</td>
<td>t-pdgs-aux-ingestion-events</td>
</tr>
<tr class="even">
<td>EDRS_SESSIONS</td>
<td>{<br />
"objectStorageKey": "{key in the object storage}",<br />
"channelId": {channel identifier},<br />
"productType": {product type: RAW or SESSION},<br />
"satelliteId": "{satellite identifier}",<br />
"missionId": "{mission identifier}"<br />
}</td>
<td>{<br />
"objectStorageKey": "S1A/L20171109175634707000125/ch01/DCS_02_ L20171109175634707000125_ch1_DSIB.xml",<br />
"channelId": 1,<br />
"productType": "SESSION",<br />
"satelliteId": "A",<br />
"missionId": "S1"<br />
}<br />
{<br />
"objectStorageKey": "S1A/L20171109175634707000125/ch01/DCS_02_ L20171109175634707000125_ch1_DSDB_00014.raw",<br />
"channelId": 1,<br />
"productType": "RAW",<br />
"satelliteId": "A",<br />
"missionId": "S1"<br />
}</td>
<td>t-pdgs-session-file-ingestion-events</td>
</tr>
<tr class="odd">
<td>PLAN_AND_REPORT</td>
<td><p>{</p>
<p>"productFamily":"PLAN_AND_REPORT",</p>
<p>"keyObjectStorage": "{key in the object storage}", </p>
<p>"uid":"{reporting task uid}",</p>
<p>"creationDate":"{event creation date}",</p>
<p>"hostname":"{event creation hostname}",</p>
<p>"productName":"{product name}",</p>
<p>"relativePath":"{file pickup path in inbox}"</p>
<p>}</p></td>
<td><p>{</p>
<p>"productFamily":"PLAN_AND_REPORT",</p>
<p>"keyObjectStorage":"S1A_OPER_REP_MP_MP__PDMC_20200303T093232_V20200303T170000_20200319T190000.xml",</p>
<p>"uid":"045fea50-0893-46ef-839b-2e095bce438a",</p>
<p>"creationDate":"2020-03-31T10:37:50.572Z",</p>
<p>"hostname":"s1pro-plan-and-report-ingestion-0",</p>
<p>"productName":"S1A_OPER_REP_MP_MP__PDMC_20200303T093232_V20200303T170000_20200319T190000.xml",</p>
<p>"relativePath":"S1A_OPER_REP_MP_MP__PDMC_20200303T093232_V20200303T170000_20200319T190000.xml"</p>
<p>}</p></td>
<td>t-pdgs-plan-and-report-ingestion-events</td>
</tr>
<tr class="even">
<td>LEVEL_JOBS</td>
<td><p>{</p>
<p>“family”: “{level job family: L0_JOB | L1_JOB}</p>
<p>"productIdentifier": "{session identifier}",</p>
<p>"workDirectory": "{local work directory for IPF execution workers}",</p>
<p>"jobOrder": "{local path of the job order XML file in the IPF execution worker}",</p>
<p>"inputs": [  {</p>
<p>  "family": "{input family: AUXILIARY_FILE | EDRS_SESSION | JOB_ORDER | L0_PRODUCT }",</p>
<p>  "localPath": "{local path of the input in the work directory},</p>
<p>  "contentRef": "{if JOB_ORDER: XML content file; if BLANK: nothing; else the key in OBS}”</p>
<p>}],</p>
<p>"outputs": [  {</p>
<p>  "family": "{product family: Lx_PRODUCT | Lx_ACN | Lx_REPORT}",</p>
<p>  "regexp":   "{regular expression for the output}"</p>
<p>}],</p>
<p>"pools": [{</p>
<p>   "tasks": [{</p>
<p>      "binaryPath": "{binary path}"</p>
<p>  }]</p>
<p>}</p></td>
<td>See annexa A</td>
<td><p>t-pdgs-aio-execution-jobs,</p>
<p>t-pdgs-l1-execution-jobs-nrt,</p>
<p>t-pdgs-l1-execution-jobs-fast</p></td>
</tr>
<tr class="odd">
<td>LEVEL_PRODUCTS</td>
<td><p>{</p>
<p>"family": "{family: Lx_SLICE | Lx_ACN | L0_BLANK}",</p>
<p>"productName": "{product name}",</p>
<p>"keyObjectStorage": "{key in the object storage}",</p>
<p>“processMode”: “NRT | FAST”</p>
<p>}</p></td>
<td><p>{</p>
<p>"family": "L0_SLICE",</p>
<p>"productName": "S1B_IW_RAW__0SSH_20180227T125035_</p>
<p>20180227T125107_009809_011BB2_38E5.SAFE",</p>
<p>"keyObjectStorage": "S1B_IW_RAW__0SSH_20180227T125035_</p>
<p>20180227T125107_009809_011BB2_38E5.SAFE",</p>
<p>“processMode”: “NRT”</p>
<p>}</p></td>
<td><p>t-pdgs-aio-l0-slice-production-events-nrt,</p>
<p>t-pdgs-l0asp-l0-slice-production-events-fast,</p>
<p>t-pdgs-aio-l0-blank-production-events</p></td>
</tr>
<tr class="even">
<td>LEVEL_SEGMENTS</td>
<td><p>{</p>
<p>"family": "{family: L0_SEGMENT}",</p>
<p>"productName": "{product name}",</p>
<p>"keyObjectStorage": "{key in the object storage}",</p>
<p>“processMode”: “NRT | FAST”</p>
<p>}</p></td>
<td><p>{</p>
<p>"family": "L0_SEGMENT",</p>
<p>"productName": "S1B_IW_RAW__0SSH_20180227T125035_</p>
<p>20180227T125107_009809_011BB2_38E5.SAFE",</p>
<p>"keyObjectStorage": "S1B_IW_RAW__0SSH_20180227T125035_</p>
<p>20180227T125107_009809_011BB2_38E5.SAFE",</p>
<p>“processMode”: “FAST24”</p>
<p>}</p></td>
<td>t-pdgs-aio-l0-segment-production-events</td>
</tr>
<tr class="odd">
<td>LEVEL_REPORTS</td>
<td>{<br />
"family": "{family: Lx_REPORT}",<br />
"productName": "{product name}",<br />
"content": "{content of the report file}",<br />
}</td>
<td></td>
<td><p>t-pdgs-aio-production-report-events,</p>
<p>t-pdgs-l1-production-report-events,</p>
<p>t-pdgs-l2-production-report-events</p></td>
</tr>
<tr class="even">
<td>EVICTION_MANAGEMENT_JOBS</td>
<td>{<br />
"productFamily": "{product family}",<br />
"keyObjectStorage": "{key in object storage}",<br />
"uid": "{reporting task uid}",<br />
"creationDate": "{event creation date}",<br />
"hostname": "{event creation hostname}",<br />
"evictionDate": "{eviction date of product in obs}",<br />
"unlimited": {true if no eviction at all}<br />
}</td>
<td>{<br />
"productFamily": "EDRS_SESSION",<br />
"keyObjectStorage": "S1A_20200120185900030888/ch02/DCS_01_S1A_20200120185900030888_ch2_DSIB.xml",<br />
"uid": "6af62c95-9bbb-4fb3-a75f-511c3b05f319",<br />
"creationDate": "2020-05-19T16:41:14.870Z",<br />
"hostname": "s1pro-datalifecycle-trigger-0",<br />
"evictionDate": "2020-01-08T21:20:15.909Z",<br />
"unlimited": false<br />
}</td>
<td>t-pdgs-eviction-management-jobs</td>
</tr>
</tbody>
</table>

<span id="_Toc256000107" class="anchor"></span>Kafka Message format per topic

### Software Trade-offs

#### KAFKA Lag balance

**The starting Situation**
Each consumer is assigned only one partition. Each partition contains the same amount of messages. This is the ideal situation as all consumer can continiously process.

However for certain activities (e.g. compression, processing) the processing time of each message can be different.
![03c96b12638be9848d95c5610a9b1245.png](media/03c96b12638be9848d95c5610a9b1245.png)




**The Problem**
Assuming that Consumer 1 and 2 are having short running messages after a while this will lead to a situation where the partitions are running empty. Consumer 3 however does have messages queued up.
![9a25a36b619097535816c8dad7413019.png](media/9a25a36b619097535816c8dad7413019.png)
Assumption would be that messags from Consumer 3 are shared across the other consumers.

**The Solution**
The producer will get a customized Partition Selection strategy so that each time a new message is selected, it will evaluate the lag that is currently on the partition. Those partition with the least lag will receive the new message.

Thus when new messages are created, all consumers will be kept busy. Other strategies like round robin, sticky partitioner and default partitioner might lead to a situation where the lag is increased and consumers might run out of tasks.

![f8903aa172989839b55598d03fa18159.png](media/f8903aa172989839b55598d03fa18159.png)
Detailed trade-off provides under: https://github.com/COPRS/production-common/wiki/Trade-off-%22Load-Balance-the-Lag%22

#### Long Running Task

![c09d05e1f2e979f6800dc3d50eb8b332.png](media/c09d05e1f2e979f6800dc3d50eb8b332.png)

#### Dateline Handling

Elasticsearch supports handling of the dateline when working with polygons.
Those polygons that cross the dateline have to be marked as such (https://www.elastic.co/guide/en/elasticsearch/reference/7.14/geo-shape.html#polygon-orientation).

The metadata extraction component detects dateline crossings and adds the specific marker when a product is cataloged.

The detector first assumes that an input polygon is not crossing the dateline and calculates its width. If the width is 180° or greater, the input polygon possibly crosses the dateline. In this case, the coordinates are relocated by shifting negative longitudes by 360.0° (resulting in only positive longitudes). If the width of the relocated polygon is lower than 180°, then an alternative smaller polygon exists for the extra wide input polygon and the detector will report a dateline crossing. If the width of the relocated polygon is 180° or greater, no decision can be made, because the input polygon and the relocated polygon both appear to possibly cross the dateline. When this countercheck fails, no crossing is reported. This is to avoid false positives (e.g. when handling wide landmasks).

#### S2 Geographical Search
#TBD
Possible approaches:
- Mission specific indices for PRIP Index
- Upgrade to Elastic Search 8.1 might improve due to Treemap algorithms
- Splitting granules into a separate index and have in the PRIP index 
just datastrips (?)


#### Error Handling


#### Error situations
When a SCDF stream is executed, multiple error might occur:
1. A routing to a target is performed that does not exist
2. The message received was not formatted correctly
3. The application itself run into an error (e.g. metadata could not extracted, IPF returned error code)

#### Dead-Letter-Queue management

The request running into an error will be send to a specific DLQ (kafka topic) and processed by the DLQ manager that will check if the error can be identified based on a ruleset.

In this case the message will be rerouted back into the kafka topic of the application so it is processed again. Each time a retry attempt will be written in the Kafka header to determinate the amount of retries to ensure messages are not kept forever in the system.

If retry limit exceeded or no rule found, the message will be send to a mission specific parking lot.

#### Parking Lot
Messages from the parking lot will be processed by the parking lot application that will push the message into a mongo database to persist it. A rest interface is exposed and allows the operator to decide how to handle with this message (drop it, restart or resubmit etc).

![DLQ_Manager.png](media/DLQ_Manager.png)

This allows the operator to investigate the reason of the failure. 

## 4 Software Component Design

4.1 [Ingestion](#ingestion)
4.2 [Processing](#processing)
4.3 [Metadata](#metadata)
4.4 [Compression](#compression)
4.5 [Distribution](#distribution)
4.6 [Data Lifecyle](#datalifecyle)
***

### Ingestion

When producing products in the Earth Observation domain there are usually a set of input files required by the processing system. These are the main input containing the actual sensor data from the satellite and on the other hand auxiliary files (e.g. predicted orbits) that are required by the processors for the computation as well.

Before the producton can be prepared it will be required to have these products available in the local environment and the system needs to be aware that they are existin. This process is referenced as "ingestion" and will be usually fetching the products mentioned above from other system and ensure they become visible for the local processing system. 

Additionally especially main input products like EDRS sessions are used to invoke systematic workflows handling the actual processing of high level products.

The COPRS the Ingestion chain is used to detect and retrieve products from other systems like CADIP, EDIP and AUXIP. The metadata extraction of these products and adding them to the catalog will be tackled in the Metadata chain.

A generic ingestion chain will be looking like this:
![edc9a8e1896c38b1d832517f81ddbde1.png](media/edc9a8e1896c38b1d832517f81ddbde1.png)

The trigger is handling the dection of new products. The filter is used to discard products that are not supposed to be processed as the COPRS will not process 100% of the data. The ingestion worker handles the actual download of the remote system into the local Object Storage.

For further information in the Ingestion Chain and an example configuration, please have a look [here](https://github.com/COPRS/production-common/tree/develop/processing-common/ingestion). Please note that additional examples of inbox configuration can be found in the [examples](https://github.com/COPRS/production-common/tree/develop/rs-core-examples)

#### Ingestion Trigger

![Screenshot_20220303_134317.png](media/Screenshot_20220303_134317.png)

Please note that the source is not part of the COPRS environment, but rther being an remote external source like:
- AUXIP (ODATA)
- CADIP (ODATA)
- EDIP (FTP)
- XBIP (WebDAV, deprecated)

The ingestion chain is a generic component and capable of handling the different kinds of protocols that are used for the sources. It is possible to setup Inbox for the Ingeston chain that will observe the specified location depending on the type of the Inbox. E.g. in case of an CADIP, Odata will be used in order to query for new session and files and retrieve it over the interface.

Because the actual retrieval of products from the source is an IO expensive activity, it was decided to split the detection of new products from the actual retrieval process. Thus the ingestion trigger will be simply in charge to poll on the source in order to detect the availability of new products that might be interesting for the COPRS environment.

It just contains a set of inboxes on the source and polling on them. If a new product is being detected, it will fire an event to notify the other components that a new product needs to be fetched.

Additionally it will make a note and store the products that had been fired already so it will be able to detect if such an event was already fired. Thus it is prevented that the same product will be fetched multiple times and keep the system unnecessarily busy.

#### Ingestion Filter

![Screenshot_20220303_134433.png](media/Screenshot_20220303_134433.png)

Especially for the CADIP sources that are retrieving the actual raw input files (EDRS Session), the COPRS shall not retrieve all available files from the source. Instead just about 3% of the products shall be processed within the COPRS.

In order to control that not all products will be processed, the ingestion filter will be a filter gate for the system to ensure that just products being detected within a time interval will be actually processed.

Each ingestion filter does have a configuration based on time defining the time window where products are accepted. Products outside of this window will be simply dropped and not pulled into the system. If the product not had been filtered, it will notify the ingestion workers.

#### Ingestion Worker

![Screenshot_20220303_134633.png](media/Screenshot_20220303_134633.png)

Products that had been detected on the source inbox and not was discarded by the filter will be handled as requests by this component and taking over the I/O intensive activity of downloading the product from the remote source into the local Object Storage.

Similiar to the Ingestion Trigger, the Ingestion Worker is a generic component that is capable of handling the different types of sources and the required protocols. It will automatically choose what kind of protocol is required in order to retrieve the product from the source location

The downloaded product is not persisted in the ingestion worker, but directly uploaded to the local Object Storage into the bucket specified for that product type. The Ingestion Workers task is just to handle the upload of the product into the OBS and is not performing any further action on the downloaded product.

Please note that when a product is ingested into the system additional metadata from the inbox might be added to the event to make the identification of the inbox for the Metadata Extraction easier.

Each worker can handle one retrieval operation at a time, so it can be scaled up in order to increase the overal throughtput of the system easily. 


#### AUXIP
The AUXIP interface is used by COPRS to poll for new AUX files and download and ingest such files. Please note that the ingestion for this workflow is a bit different than these from XBIP and EDIP, because the products retrieved from the AUXIP are compressed files. The environment expects the products to be uncompressed to allow partial file download (e.g. Manifest) and because most processors will require it.

The AUXIP ingestion trigger component polls configured PRIP interfaces per AUXIP client (OData) to detect new AUX. Due to the nature it is not quering all products it is finding, but using a moving time window to go throught the products in order to identify products that are not pulled into the system already.

Once it reaches the current time, it will stay up-to-date following all new publications on the PRIPs as long as it is running. It will then create ingestion jobs for the AUXIP ingestion worker which will then download the files from PRIP and uploads it to OBS storage. The AUXIP ingestion trigger uses MongoDB to persist metadata about the harvested timewindow to be able to resume operation were it last left off.

Note that the products retrieved from the AUXIP are compressed and needs to be uncompressed before it can be used by the COPRS environment. Thus it is required to execute at the end of the SCDF stream an uncompression application. An example on how to configure the AUXIP chain correctly, can be found [here](https://github.com/COPRS/production-common/tree/develop/rs-core-examples/ingestion-auxip)

Credentials to log into PRIP services (OData) are configured by Kubernetes secrets and are provided to the AUXIP client as needed. Different credentials for each PRIP service are supported

#### CADIP

The CADIP interface is the sucessor of the XBIP interface and is supposed to be used in order to retrieve the CADU files for the local processing system. Similiar to the AUXIP interface CADIP is based on OData4 and thus not just allow to download products, but also to perform Queries for sessions and files.

The CADIP trigger is polling the specified endpoint for new sessions to appear. If a new session is identified it will be persisted. When quering for new sessions the publication date will be considered and just the trigger will just look for new sessions after the last finding to avoid retrieving old sessions all the time.

Once a session is identified, the trigger will also start to query for the files that are contained within the session. Each file that is found will be fired as new event that will invoke the handling of the download into the local processing system. This will be continued per channel as long as no chunk is detected having the final block flag set.

Each file identified by the trigger is send to the filter that can decide to drop the file or to provide it to the worker. This can be used in order to ensure that files just within a specific time interval are accepted.

Once receiving a download request, the worker will contact the CADIP endpoint and query the file to get its UUID. Then a download is invoked from the OData interface and the provided file will be automatically uploaded into the Object Storage of the local processing system. A Catalog Event is written to invoke the metadata extraction from the file and addin it to the catalog to make it available to the processing chains.

Note that to keep the RS-Addons behave the same way as before the worker will create a DSIB per channel once the files with the final block flag had been received. This DSIB file is just used to trigger the workflows.


#### XBIP
*Please be aware that this interface is deprecated and will be replaced by the CADIP in order to retrieve CADU sessions. It should not be used in new configurations anymore*

Ingestion-Trigger component polls XBIP interface (webdav) of ground station and detects new files (CADU & DSIB). Once a new file is detected, an ingestion job is created and send to ingestion-workers. 

To be able to detect files as "new", ingestion-trigger stores in a persistence a last-known list of files available in XBIP and can compare current content with last-known content. This is valid for nominal and retransfer folders as both will be polled systematically. IngestionJobs are put on the nominal (high-priority) or retransfer (low-priority) queue depending on their origin.

Ingestion-worker component executes an ingestion job (containing a URL, the OBS target and some additional attributes about the ground station) by downloading the file from URL and uploading to OBS. For files from retransfer a dedicated bucket is used.

One ingestion-trigger pod per XBIP station to avoid dependencies between the stations. If one station is having high lag or any other kind of connection issues, this shall not cause any negative influence on data flows from other stations.

Credentials to log into XBIP stations (webdav) are configured by Kubernetes secrets and are provided to ingestion trigger and worker as needed. It is supported to different credentials for each station.

#### EDIP
The EDIP interface is used by COPRS services to poll for new CADU chunks and DSIB files and download and ingest such files via FTP protocol. The interface is specified in the ICD "EDRS-ICD-ADSO-1000830938"

Features:
- The directory structure of the  files is identical to the XBIP Interface.
- Interface protocol is FTP
- Directory for polling on the "Primary EDC"  is NOMINAL directory, no RETRANSFER directory present on the same server
- Authentication factor is user/password: handled in COPRS
  
### Processing

Each time a product will be added to the Metadata Catalog (nevertheless if it is ingested or produced) a catalog event will be raised to notify all components that a new product is available.

Each chain is having a filter as gate that will decide if it is responsible for doing the production. A preparation worker will verify if all preconditions are meet and generating a job order that shall be executed by an Execution Worker.

![1b54a172e7dcce316195e1a06408bf45.png](media/1b54a172e7dcce316195e1a06408bf45.png)

Because the origin topic will be always the catalog events, it is also possible to chain multiple processings in a systematic way. The following figure is giving an overview how the S1 AIOP and S1 L1 could be realized in the new system:

![Screenshot_20220304_105218.png](media/Screenshot_20220304_105218.png)



#### Filter

Each processing chain will be triggered when a new product is available and starting with a filter component that will be used as a gate for the chain and its task is to ensure that just events are processed they can handle. 

The filter is supposed to be lightweight and working in a similiar way than the MQI filter mechanism that had been used before. So product family, product type and mission might be the main criteria to decide if the chain is able to handle this processing. The filter is not supposed to contain more complex processing logic (e.g. if all chunks of a session are available). The only purpose is to define if the chain can be used to perform the processing.

#### Preparation Worker
If a new request for a main input product is detected, it will perform a check if its mandatory input files are already existing. If its the case, it can directly generate the job order and continue. If at the moment however not all criteria are fullfilled, it will store the job into a job database.

This will not be working in a global way anymore, but instead each processing chain does have its own database to ensure that the processing requests not interfer with each other.

The persistence is required to ensure that the jobs can be handled in a different order than they arrive at the kafka partitions they are reading from. Else it might happen that if an auxiliary data is not arrived correctly, the system will not continue with other jobs that might be ready to start already. This is not wanted behaviour as the production shall be invoked whenever  ready.

After the new job is persisted, the service will also check if other old job that are persisted might be meanwhile be ready and can be launched. If no production is ready, the workflow will be discarded and discontinued.

Thus each time a new catalog event arrived, the preparation worker is invoked and checking if preconditions are fullfilled. Using this approach it is not longer necessary to use a time based polling. Because if no new products are added to the system, the precondition for the inputs will not be different.

Once a production is ready to go, the Preparation Worker will generate a Job Order containing the inputs that had been choosed for the production and will publish it on the queue of the execution worker, so it can do the actual processing.


#### Execution Worker
While all services before are basically organize and setup the production, the execution worker is the service that is performing the actual processing operation. It is however also in charge to prepare the working directory and monitoring the results of the production.


![Screenshot_20220304_100505.png](media/Screenshot_20220304_100505.png)

The execution worker will receive an job from the preparation worker. At this moment all decision about the input products had been made already and are listed within the Job Order. The execution worker will not do any further decision on what to produce, but just following the instructions from the order.

It initialize a clean working directory that will be used as processing environment by the IPF. The input products that had been selected by the Preparation Worker and downloaded from the OBS storage from the associated buckets and placed within the working directory.

When the production is prepared, the Execution Worker will launch the binary from the task table to invoke the actual production by executing the CFI binary within the working directory and providing the Job Orders as input.

The binary takes over the control and the execution worker will read the stdout messages and logging them accordingly. Once the binary is finished, it will return an exit code and indicate the outcome of the production. 

If error code 0 is returned, it means that the production was successful. A .LIST file will be generated to list the products that had been produced and needs to be extracted. If an higher exit code is returned this might indicate an issue. An exit code of 1-127 might indicate a warning, but is considered still successful. An even higher exit code might indicate instead an error, so that the production is not generated correctly.

In this case, the Execution Worker will not continue with the processing, but publish an error on the Dead Letter Queue to raise the attention of the operator team to investigate the reasons for this failure.

If the production was successful, the Execution worker will parse the content of the .LIST file and starting to upload the produced outputs into the OBS storage into the associated buckets.

After the upload, it will clean up the working directory to ensure no disk space leaks might occur and publishing a new production event. At this moment the products are not visible for the system and the metadata catalog will, handle the extraction of the metadata of the freshly generated products and making them visible after they had been added to the catalog.

### Metadata

When a new product was ingested into the COPRS environment it will be just available at the OBS store, however the system will not be immediatly aware of these products. It is required to extract the metadata of the products first and store them into the local catalog to make them visible for the overall system.

This is also required when a product was not ingested, but had been produced by a processing chain. The product will be uploaded to the OBS, but will not be visible for the overal system. Thus its metadata needs to be extracted.

The Metadata Extraction chain is tackling exactly this activity. Because the different product types and missions are having different types of products, the Metadata extraction needs to be suitable for the different formats and store them in an homogenous catalog, so it is possible to access the product metadata in the same way from all systems.
![11d2bbd4db0704c5241130390912912a.png](media/11d2bbd4db0704c5241130390912912a.png)

#### Filter

The filter within the RS Core Metadata chain can be used as a gate to specify what products shall be ingested into the Metadata Catalog. This can be used to avoid that products like reports are actually added into the catalog. Additionally it can be used to specify a mission specific metadata extraction, e.g. if it is required to scale the amount of extractors on a mission base. In this case the filter can be configured to just accept a specific mission.

The message filter is a basic SCDF component that allows to filter out products based on the payload of the event.

#### Extraction

The SCDF application Extraction takes the product from the received event and will identify the type of extraction that is required for them. It will then fetch the metadata from the OBS (e.g. the manifest file for a SAFE product) and start with the extraction of the metadata. The result will be stored within the Elastic Search index related to the product and it will become visible for other application within the system. Products that are not extracted yet, will not be visible e.g. via the Metadata Search Controller.

### Compression

![e830e7833cab916176c295924f8437bb.png](media/e830e7833cab916176c295924f8437bb.png)

Earth observation products can be either single files or directory products that contains multiple files in an organized way. Especially during download these directory products can be a problem as the End User usually just wants to download a single file as it is easier ot handle and is faster than downloading multiple small files sequentially.

Thus the compression chain can be used to compress a product. It will be listening on "Catalog Events" for new products. A generic SCDF filter is running in the front and allows to specifiy Product Families or file name patterns of products that shall be handled by the Compression chain. This allows to discard products that are not supposed to be compressed and then published on the DDIP index. Thus the filter is working like a gate that will just let products pass that shall be compressed.

Because products can be easily multiple Gigabyte in size, the compressions does usually take a few seconds. To ensure that products with low priorities are not blocking these with a high priority, the Stream splits into three different priorities:
* low
* medium
* high

Each priority will be gated by another filter that allows to specify the criteria for the priority. In most cases it can be assumed that the timeliness from the event will be the criteria to determinate the priority of the requests. E.g. in S1 if it is "FAST24" it would be a high priority, while NRT would be medium priority. If no timeliness is provided the low priority chain can be performed.

On each of the priority chains individual compression worker can be configured to scale the amount of workers for certain priorities. E.g. it might be wanted to run more workers for the high priority chain than for the lower ones to ensure that products are processed in a faster manner and not queued up for a long time.

The Compression worker itself will download the product into a local working directory, performing the compression command that had been configured on it and uploading the compressed product into the OBS. Please note that this will change the product family as well as a "-zip" suffix will be added to indicate the product is a compressed one.

At the end of the chain a "Compression Event" will be generated that will be usually consumed by the Distribution chain handling the publication of the product and make it available for the End users.

For further information on the compression chain, please have a look [here](https://github.com/COPRS/production-common/tree/develop/processing-common/compression)

#### Message Filter
The message filter is a basic SCDF component that allows to filter out products that are not supposed to be compressed.

#### Priority Filter
Similiar to the message filter, the priority filter is also a basic SCDF component that is supposed to split the requests into different priority chains. Each priorty filter defines what kind of criteria they would like to have to assume a certain priority for the compression. E.g. if a timeliness "FAST24" is defined, the request will be assumed to be high priority. Please ensure that all filters are not overlapping each others criteria and each filter can be assigned only to a single priority.

#### Compression Worker

The compression worker is performing the actual compression of the product. It will take the product referenced in the catalog event, download it into a local working directory and execute the configured compression script to perform the compression.

The kind of compression that is used is depending on the mission. The compression worker will read the missionId from the Catalog Event. Depending on the mission it is possible to define a compression script that shall be used. By default S1 will use compressed ZIP, S2 will use tar and S3 will use uncompressed ZIP. For further information on the configuration, please consult the parameter configuration for the Compression worker that can be found [here](/processing-common/compression/doc/ReleaseNote.md)

After the compression had been performed successfully the result will be uploaded into the OBS. Please note that it will not be uploaded into the same bucket as the original product. Instead the bucket and product family will add a suffix "-zip" to indicate it actually references a compressed product and to distinguish between these.

### Distribution

![905146a466c8f8dd944cec89e955c99f.png](media/905146a466c8f8dd944cec89e955c99f.png)

The Distribution chain is resposible for publishing products that had been produced within the COPRS environment to the published on the PRIP index. After it was published, it will be available for the external users of the system via the DDIP interface or the native API or searchable in the User Web Client.

Because products that are downloadable via the DDIP are usually compressed products as handling directory products can be tricky, the distribution chain is usually executed after the Compression chain had been performed. Thus the chain listens on "compression-events" and just having a single sink that handled the publication.

The Distribution worker will take the product from the event and perform a query on the Metadata Catalog Search Controller to query the metadata of the uncompressed product from the Metadata Catalog. A DDIP compatible entry will be generated and published on the index making the product available.

After the publication no further steps are performed. Thus this chain is usually a endpoint that does not triggering any further activities. Once a product had been produced by the processing chain, it will be ingested into the Metadata Catalog

The products produced by the IPFs will be in general directory prodcuts containing multiple files. This is making the download of these products difficult. Thus there exists a compression chain that will take a normal product from the OBS and performing a compression option on it to ZIP the product into a single archieve.

For further information on the Distribution chain, please have a look [here](https://github.com/COPRS/production-common/tree/develop/processing-common/distribution)

#### Distribution Worker
The Distribution worker handled the publication of products on the DDIP index in order to make them queriable for external users. It's only task is to handle the registration of the products and no other activities are performed on the product itself.

As download URL it will take the compressed products that had been placed while the product had been processed by the compression Chain.

#### DDIP Frontend
While the Distribution Worker will just handle the generation of the entries in the DDIP index, the [DDIP Frontend](/production-common/tree/develop/rs-processing-common/ddip_frontend) will handle incoming requests for the users or other systems. 

Please note that it is not integrated as SCDF stream as it is not an event based component, but is [deployed](/production-common/tree/develop/rs-processing-common) as a Helm Chart as standalone component.

It will then provide a [OData](https://www.odata.org/) interface that can be used to query on the index. The queries are handled and queries on the PRIP index within the Elastic Search cluster are performed to return the results to the end user.

### DataLifecyle
![dlm_overview.png](media/dlm_overview.png)

The Datalifecycle chain is responsible for memorizing the point of time when files enter the system or are compressed, and for removing expired files and its metadata.

On each Catalog and Compression Event, the Data Lifecycle Worker calculates an expiration date for the file of the event in consulting the configured retentions policies and updates its index for that file. The part of DLM responsible for eviction is provided by the Eviction Manager service that is running independently. It works with a scheduler that runs periodically and checks for expired files and removes them from the Object Store, Metadata Catalog and PRIP index accordingly.

Persistence backend for the Datalifecycle is an Elasticsearch index named 'data-lifecycle-metadata' where all data relevant for eviction is stored:

| Metadata | Details |
|-|-|
| ProductName | Name of the product |
| PathInUncompressedStorage | OBS path. If null/empty file is not available in uncompressed storage, evicted or just being stored and the Data Lifecycle Worker was not informed yet |
| ProductFamilyInUncompressedStorage | Product family of product stored in the uncompressed storage (e.g. AUXILIARY_FILE) |
| LastInsertionInUncompressedStorage | Last date the product was stored in the uncompressed storage |
| EvictionDateInUncompressedStorage | Date of eviction from the uncompressed storage and Metadata Catalog |
| PathInCompressedStorage | OBS path. If null/empty file is not available in compressed storage, evicted or just being stored and the Data Lifecycle Worker was not informed yet |
| ProductFamilyInCompressedStorage | Product family of product stored in the compressed storage (e.g. AUXILIARY_FILE_ZIP) |
| LastInsertionInCompressedStorage | Last date the product was stored in the compressed storage |
| EvictionDateInCompressedStorage | Date of eviction from the compressed storage and PRIP index |
| LastModified | Last date the product is updated (inserted into storage or evicted) |
| AvailableInLta | Will remain empty. This is taken over from S1 context, but is not used in the Reference System at the moment |
| LastDataRequest | Will remain empty. This is taken over from S1 context, but is not used in the Referenc System at the moment |
| PersistentInUncompressedStorage | Will remain empty. This is taken over from S1 context, but is not used in the Reference System at the moment |
| PersistentInCompressedStorage | Will remain empty. This is taken over from S1 context, but is not used in the Reference System at the moment |
