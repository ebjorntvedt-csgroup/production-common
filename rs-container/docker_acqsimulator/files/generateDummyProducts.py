#!/usr/bin/env python2


import os
import datetime

import xml.etree.ElementTree as ET

# Generate an IIF-XML-File


def generateIIFXML(productType, productName, startTime, stopTime, groundStation, recStartTime, recStopTime, number, position, dumpStart, provider, outputfile):
    root = ET.Element('IIF')
    root.set('xmlns:java', 'http://xml.apache.org/xaLan/java')
    root.set('xmlns:gml', 'http://www.opengis.net/gml')
    root.set('xmlns:safe', 'http://www.esa.int/safe/1:2')
    item = ET.SubElement(root, 'item')

    createAdministration(item, productType, productName)
    createFileInformation(item, productName)
    createParameters(item, startTime, stopTime)
    createSpecificParameters(item, groundStation, recStartTime,
                             recStopTime, number, position, dumpStart, provider)

    et = ET.ElementTree(root)
    myfile = open(outputfile, "w")
    et.write(myfile, encoding='UTF-8')

# Create administration Part of IIF file


def createAdministration(item, productType, productName):
    administration = ET.SubElement(item, 'administration')
    admId = ET.SubElement(administration, 'id')
    admId.text = '(rid://ingest)'
    admType = ET.SubElement(administration, 'type')
    admType.text = productType
    keys = ET.SubElement(administration, 'keys')
    feature1 = ET.SubElement(keys, 'feature')
    feature1.set('key', 'originalName')
    feature1.text = productName
    feature2 = ET.SubElement(keys, 'feature')
    feature2.set('key', 'productType')
    feature2.text = productType
    feature3 = ET.SubElement(keys, 'feature')
    feature3.set('key', 'code')
    feature3.text = productType

# Create fileInformation Part of IIF file


def createFileInformation(item, productName):
    fileInformation = ET.SubElement(item, 'fileInformation')
    file1 = ET.SubElement(fileInformation, 'file')
    location = ET.SubElement(file1, 'location')
    name = ET.SubElement(location, 'name')
    name.text = productName

# Create parameters Part of IIF file


def createParameters(item, startTime, stopTime):
    parameters = ET.SubElement(item, 'parameters')
    tempCoverage = ET.SubElement(parameters, 'temporalCoverage')
    startTimeField = ET.SubElement(tempCoverage, 'startTime')
    startTimeField.text = startTime
    stopTimeField = ET.SubElement(tempCoverage, 'stopTime')
    stopTimeField.text = stopTime
    quality = ET.SubElement(parameters, 'quality')
    quality.text = 'APPROVED'

# Create specificParameters Part of IIF file


def createSpecificParameters(item, groundStation, recStartTime, recStopTime, number, position, dumpStart, provider):
    specificParameters = ET.SubElement(item, 'specificParameters')
    feature1 = ET.SubElement(specificParameters, 'feature')
    feature1.set('key', 'receivingGroundStation')
    feature1.text = groundStation
    feature2 = ET.SubElement(specificParameters, 'feature')
    feature2.set('key', 'receivingStartTime')
    feature2.text = recStartTime
    feature3 = ET.SubElement(specificParameters, 'feature')
    feature3.set('key', 'receivingStopTime')
    feature3.text = recStopTime
    feature4 = ET.SubElement(specificParameters, 'feature')
    feature4.set('key', 'granuleNumber')
    feature4.text = number
    feature5 = ET.SubElement(specificParameters, 'feature')
    feature5.set('key', 'granulePosition')
    feature5.text = position
    feature6 = ET.SubElement(specificParameters, 'feature')
    feature6.set('key', 'dumpStart')
    feature6.text = dumpStart
    feature7 = ET.SubElement(specificParameters, 'feature')
    feature7.set('key', 'ISIPProvider')
    feature7.text = provider

# Generate product name


def generateProductName(satelliteId, productType, startTime, stopTime, created=datetime.datetime.now()):
    return satelliteId + "_" + productType + "_" + startTime.strftime("%Y%m%dT%H%M%S") + "_" + stopTime.strftime("%Y%m%dT%H%M%S") + "_" + created.strftime("%Y%m%dT%H%M%S") + "___________________WER_D_NR____"

# Generate folder Structure


def generateFolderStructure(localWorkingDir, productName):

    parentDir = os.path.join(localWorkingDir, productName + ".ISIP")
    childDir = os.path.join(parentDir, productName)

    os.mkdir(parentDir)
    os.mkdir(childDir)

    f = open(os.path.join(childDir, "ISPData.dat"), "a")
    f.write("This product was generated by the S3ACQSimulator")
    f.close()

    f = open(os.path.join(childDir, "ISPData.xsd"), "a")
    f.write("This product was generated by the S3ACQSimulator")
    f.close()

    return parentDir

# Generate DO_0_DOP products


def generateDODOP(startTime, satelliteId, localWorkingDir):
    stopTime = startTime + datetime.timedelta(seconds=6155)
    dumpStart1 = startTime - datetime.timedelta(seconds=4477)
    dumpStart2 = startTime + datetime.timedelta(seconds=1583)

    productType = "DO_0_DOP__G"
    productName1 = generateProductName(
        satelliteId, productType, startTime, dumpStart2)
    productName2 = generateProductName(
        satelliteId, productType, dumpStart2, stopTime)

    receiveStartTime = stopTime
    receiveStopTime = receiveStartTime + datetime.timedelta(seconds=89)

    productFolder1 = generateFolderStructure(localWorkingDir, productName1)
    productFolder2 = generateFolderStructure(localWorkingDir, productName2)
    generateIIFXML(productType, productName1, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "LAST", dumpStart1.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder1 + "/" + productName1 + "_iif.xml")
    generateIIFXML(productType, productName2, dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "FIRST", dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder2 + "/" + productName2 + "_iif.xml")

# Generate DO_0_NAV products


def generateDONAV(startTime, satelliteId, localWorkingDir):
    stopTime = startTime + datetime.timedelta(seconds=6150)
    dumpStart1 = startTime - datetime.timedelta(seconds=4477)
    dumpStart2 = startTime + datetime.timedelta(seconds=1583)

    productType = "DO_0_NAV__G"
    productName1 = generateProductName(
        satelliteId, productType, startTime, dumpStart2)
    productName2 = generateProductName(
        satelliteId, productType, dumpStart2, stopTime)

    receiveStartTime = stopTime
    receiveStopTime = receiveStartTime + datetime.timedelta(seconds=89)

    productFolder1 = generateFolderStructure(localWorkingDir, productName1)
    productFolder2 = generateFolderStructure(localWorkingDir, productName2)
    generateIIFXML(productType, productName1, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "LAST", dumpStart1.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder1 + "/" + productName1 + "_iif.xml")
    generateIIFXML(productType, productName2, dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "FIRST", dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder2 + "/" + productName2 + "_iif.xml")

# Generate GN_0_GNS products


def generateGNGNS(startTime, satelliteId, localWorkingDir):
    stopTime = startTime + datetime.timedelta(seconds=6155)
    dumpStart1 = startTime - datetime.timedelta(seconds=4477)
    dumpStart2 = startTime + datetime.timedelta(seconds=1583)

    productType = "GN_0_GNS__G"
    productName1 = generateProductName(
        satelliteId, productType, startTime, dumpStart2)
    productName2 = generateProductName(
        satelliteId, productType, dumpStart2, stopTime)

    receiveStartTime = stopTime
    receiveStopTime = receiveStartTime + datetime.timedelta(seconds=89)

    productFolder1 = generateFolderStructure(localWorkingDir, productName1)
    productFolder2 = generateFolderStructure(localWorkingDir, productName2)
    generateIIFXML(productType, productName1, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "LAST", dumpStart1.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder1 + "/" + productName1 + "_iif.xml")
    generateIIFXML(productType, productName2, dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "FIRST", dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder2 + "/" + productName2 + "_iif.xml")

# Generate MW_0_MWR products


def generateMWMWR(startTime, satelliteId, localWorkingDir):
    stopTime = startTime + datetime.timedelta(seconds=6155)
    dumpStart1 = startTime - datetime.timedelta(seconds=4477)
    dumpStart2 = startTime + datetime.timedelta(seconds=1583)

    productType = "MW_0_MWR__G"
    productName1 = generateProductName(
        satelliteId, productType, startTime, dumpStart2)
    productName2 = generateProductName(
        satelliteId, productType, dumpStart2, stopTime)

    receiveStartTime = stopTime
    receiveStopTime = receiveStartTime + datetime.timedelta(seconds=89)

    productFolder1 = generateFolderStructure(localWorkingDir, productName1)
    productFolder2 = generateFolderStructure(localWorkingDir, productName2)
    generateIIFXML(productType, productName1, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "LAST", dumpStart1.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder1 + "/" + productName1 + "_iif.xml")
    generateIIFXML(productType, productName2, dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "FIRST", dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder2 + "/" + productName2 + "_iif.xml")

# Generate OL_0_CR products


def generateOLCR(timestamp, satelliteId, localWorkingDir):
    startTime = timestamp + datetime.timedelta(seconds=654)
    stopTime = startTime + datetime.timedelta(seconds=2636)
    productType = "OL_0_CR___G"
    productName = generateProductName(
        satelliteId, productType, startTime, stopTime)

    receiveStartTime = timestamp + datetime.timedelta(seconds=6155)
    receiveStopTime = receiveStartTime + datetime.timedelta(seconds=89)

    productFolder = generateFolderStructure(localWorkingDir, productName)
    generateIIFXML(productType, productName, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "BOTH", startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder + "/" + productName + "_iif.xml")

# Generate OL_0_EFR products


def generateOLEFR(timestamp, satelliteId, localWorkingDir):
    productType = "OL_0_EFR__G"
    startTime = timestamp + datetime.timedelta(seconds=654)
    dumpStart = startTime
    receiveStartTime = timestamp + datetime.timedelta(seconds=6155)
    receiveStopTime = receiveStartTime + datetime.timedelta(seconds=89)

    time = 0
    number = 1
    maxTime = 2636
    position = "FIRST"
    length = 120

    while time < maxTime:
        if time + length > maxTime:
            position = "LAST"
            length = maxTime - time

        stopTime = startTime + datetime.timedelta(seconds=length)
        productName = generateProductName(
            satelliteId, productType, startTime, stopTime)
        productFolder = generateFolderStructure(localWorkingDir, productName)
        generateIIFXML(productType, productName, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                       receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), str(number), position, dumpStart.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder + "/" + productName + "_iif.xml")

        position = "NONE"
        number += 1
        startTime = stopTime
        time += length

# Generate SL_0_SLT products


def generateSLSLT(startTime, satelliteId, localWorkingDir):
    productType = "SL_0_SLT__G"
    receiveStartTime = startTime + datetime.timedelta(seconds=6155)
    receiveStopTime = receiveStartTime + datetime.timedelta(seconds=89)
    dumpStart1 = startTime - datetime.timedelta(seconds=4477)
    dumpStart2 = startTime + datetime.timedelta(seconds=1583)

    time = 0
    number = 1
    maxTime = 6155
    position = "NONE"
    length = 300
    dump1 = True

    while time < maxTime:
        if time + length > maxTime:
            length = maxTime - time

        stopTime = startTime + datetime.timedelta(seconds=length)
        # When we reached the new dump start -> split granule
        if stopTime > dumpStart2 and dump1:
            stopTime = dumpStart2
            position = "LAST"

        productName = generateProductName(
            satelliteId, productType, startTime, stopTime)
        productFolder = generateFolderStructure(localWorkingDir, productName)

        if dump1:
            generateIIFXML(productType, productName, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                           receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), str(number), position, dumpStart1.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder + "/" + productName + "_iif.xml")
        else:
            generateIIFXML(productType, productName, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                           receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), str(number), position, dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder + "/" + productName + "_iif.xml")

        # When we reached the new dump start -> use second timestamp in xml manifest
        if stopTime == dumpStart2:
            dump1 = False
            position = "FIRST"
        else:
            position = "NONE"

        number += 1
        startTime = stopTime
        time += length

# Generate SR_0_SRA products


def generateSRSRA(startTime, satelliteId, localWorkingDir):
    productType = "SR_0_SRA__G"
    receiveStartTime = startTime + datetime.timedelta(seconds=6155)
    receiveStopTime = receiveStartTime + datetime.timedelta(seconds=89)
    dumpStart1 = startTime - datetime.timedelta(seconds=4477)
    dumpStart2 = startTime + datetime.timedelta(seconds=1583)

    time = 0
    number = 1
    maxTime = 6155
    position = "NONE"
    length = 600
    dump1 = True

    while time < maxTime:
        if time + length > maxTime:
            length = maxTime - time

        stopTime = startTime + datetime.timedelta(seconds=length)
        # When we reached the new dump start -> split granule
        if stopTime > dumpStart2 and dump1:
            stopTime = dumpStart2
            position = "LAST"

        productName = generateProductName(
            satelliteId, productType, startTime, stopTime)
        productFolder = generateFolderStructure(localWorkingDir, productName)

        if dump1:
            generateIIFXML(productType, productName, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                           receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), str(number), position, dumpStart1.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder + "/" + productName + "_iif.xml")
        else:
            generateIIFXML(productType, productName, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                           receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), str(number), position, dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder + "/" + productName + "_iif.xml")

        # When we reached the new dump start -> use second timestamp in xml manifest
        if stopTime == dumpStart2:
            dump1 = False
            position = "FIRST"
        else:
            position = "NONE"

        number += 1
        startTime = stopTime
        time += length

# Generate TM_0_HKM products


def generateTMHKM(startTime, satelliteId, localWorkingDir):
    stopTime = startTime + datetime.timedelta(seconds=6155)
    dumpStart1 = startTime - datetime.timedelta(seconds=4477)
    dumpStart2 = startTime + datetime.timedelta(seconds=1583)

    productType = "TM_0_HKM__G"
    productName1 = generateProductName(
        satelliteId, productType, startTime, dumpStart2)
    productName2 = generateProductName(
        satelliteId, productType, dumpStart2, stopTime)

    receiveStartTime = stopTime
    receiveStopTime = receiveStartTime + datetime.timedelta(seconds=89)

    productFolder1 = generateFolderStructure(localWorkingDir, productName1)
    productFolder2 = generateFolderStructure(localWorkingDir, productName2)
    generateIIFXML(productType, productName1, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "LAST", dumpStart1.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder1 + "/" + productName1 + "_iif.xml")
    generateIIFXML(productType, productName2, dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "FIRST", dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder2 + "/" + productName2 + "_iif.xml")

# Generate TM_0_HKM2 products


def generateTMHKM2(startTime, satelliteId, localWorkingDir):
    stopTime = startTime + datetime.timedelta(seconds=6155)
    dumpStart1 = startTime - datetime.timedelta(seconds=4477)
    dumpStart2 = startTime + datetime.timedelta(seconds=1583)

    productType = "TM_0_HKM2_G"
    productName1 = generateProductName(
        satelliteId, productType, startTime, dumpStart2)
    productName2 = generateProductName(
        satelliteId, productType, dumpStart2, stopTime)

    receiveStartTime = stopTime
    receiveStopTime = receiveStartTime + datetime.timedelta(seconds=89)

    productFolder1 = generateFolderStructure(localWorkingDir, productName1)
    productFolder2 = generateFolderStructure(localWorkingDir, productName2)
    generateIIFXML(productType, productName1, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "LAST", dumpStart1.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder1 + "/" + productName1 + "_iif.xml")
    generateIIFXML(productType, productName2, dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "FIRST", dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder2 + "/" + productName2 + "_iif.xml")

# Generate TM_0_NAT products


def generateTMNAT(startTime, satelliteId, localWorkingDir):
    stopTime = startTime + datetime.timedelta(seconds=6142)
    dumpStart1 = startTime - datetime.timedelta(seconds=4477)
    dumpStart2 = startTime + datetime.timedelta(seconds=1583)

    productType = "TM_0_NAT__G"
    productName1 = generateProductName(
        satelliteId, productType, startTime, dumpStart2)
    productName2 = generateProductName(
        satelliteId, productType, dumpStart2, stopTime)

    receiveStartTime = stopTime
    receiveStopTime = receiveStartTime + datetime.timedelta(seconds=89)

    productFolder1 = generateFolderStructure(localWorkingDir, productName1)
    productFolder2 = generateFolderStructure(localWorkingDir, productName2)
    generateIIFXML(productType, productName1, startTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "LAST", dumpStart1.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder1 + "/" + productName1 + "_iif.xml")
    generateIIFXML(productType, productName2, dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), stopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "CGS", receiveStartTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"),
                   receiveStopTime.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "1", "FIRST", dumpStart2.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), "L0PP", productFolder2 + "/" + productName2 + "_iif.xml")


def generateDummyProducts(timestamp, satelliteId, localworkdir):
    timestamp = datetime.datetime.strptime(timestamp, '%Z=%Y-%m-%dT%H:%M:%S')

    generateDODOP(timestamp, satelliteId, localworkdir)
    generateDONAV(timestamp, satelliteId, localworkdir)
    generateGNGNS(timestamp, satelliteId, localworkdir)
    generateMWMWR(timestamp, satelliteId, localworkdir)
    generateOLCR(timestamp, satelliteId, localworkdir)
    generateOLEFR(timestamp, satelliteId, localworkdir)
    generateSLSLT(timestamp, satelliteId, localworkdir)
    generateSRSRA(timestamp, satelliteId, localworkdir)
    generateTMHKM(timestamp, satelliteId, localworkdir)
    generateTMHKM2(timestamp, satelliteId, localworkdir)
    generateTMNAT(timestamp, satelliteId, localworkdir)
