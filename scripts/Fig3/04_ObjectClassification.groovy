// Import QuPath's dependencies of relevance
import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.scripting.QP
import qupath.lib.gui.commands.Commands
import qupath.lib.roi.ROIs
import qupath.lib.roi.RoiTools
import qupath.lib.objects.PathObjects
import qupath.lib.objects.classes.PathClassTools
import qupath.lib.regions.ImagePlane
import qupath.imagej.gui.ImportRoisCommand
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport
import qupath.lib.objects.PathObjects
import qupath.lib.objects.PathObjectTools
import qupath.lib.roi.PolygonROI
import qupath.lib.images.servers.PixelCalibration

/*************************************************************************************************/
// User's input to define variables:

    /* (1) For cell detection, define the common variables */ 
        def nucleusChannelName = "DAPI"
        def celldetectionThreshold = 10.0
        def minNucleusArea = 15.0
        def maxNucleusArea = 200.0

        // Define the existing object classifier : single e.g. "Iba1+ microglia" OR composite(multiple) e.g. "microglia" 
        def cellObjectClassifierName = "microglia"
        def cell1Class = "P2RY12"
        def cell2Class = "P2RY12: Iba1"
        def cell3Class = "P2RY12: CD74: Iba1"
        def cell4Class = "Iba1: CD74"
        def cell5Class = "Iba1"
 
   /* (3) If exist, rename objects (anno/cell) with prefix(string) and/or suffix(number) */ 
        // def annoPrefix = "plaque"
        def cell1Prefix = "P2RY12+microglia_"
        def cell2Prefix = "P2RY12+Iba1+microglia_"
        def cell3Prefix = "P2RY12+CD74+Iba1microglia_"
        def cell4Prefix = "Iba1+CD74+microglia"
        def cell5Prefix = "microglia"
 
///////////////////////////////////////////////////////////////////////////////////////////////////////

        /* Cell detection */
        // Select full image ROI as parent object to detect cells from within, and rename DAPI channel
            setChannelNames('DAPI')
            selectObjectsByClassification("border");
            // run cell detection by thresholding nucleus channel, with parameters configured as below (user-customised)
            runPlugin('qupath.imagej.detect.cells.WatershedCellDetection','{"detectionImage":'+nucleusChannelName+',"requestedPixelSizeMicrons":0.5,"backgroundRadiusMicrons":8.0,"backgroundByReconstruction":true,"medianRadiusMicrons":0.0,"sigmaMicrons":1.5,"minAreaMicrons":'+minNucleusArea+',"maxAreaMicrons":'+maxNucleusArea+',"threshold":'+celldetectionThreshold+',"watershedPostProcess":true,"cellExpansionMicrons":0.0,"includeNuclei":false,"smoothBoundaries":true,"makeMeasurements":true}')
            
            // run defined threshold-based object classifiers --> give each cell detection a class e.g. microglia for Iba1+, neuron for NeuN+
             runObjectClassifier(cellObjectClassifierName);
             
            //* call function "renameObjects" as shown below, to rename target objects based on defined object type and class */
              // for cell detections like microglia, rename with prefix and numerical suffix
            renameObjects("cell", cell1Class, cell1Prefix)
            renameObjects("cell", cell2Class, cell2Prefix)
            renameObjects("cell", cell3Class, cell3Prefix)
            renameObjects("cell", cell4Class, cell4Prefix)
            renameObjects("cell", cell5Class, cell5Prefix)

               resolveHierarchy()
               fireHierarchyUpdate()
               
///////////////////////////////////////////////////////////////////////////////////////////////////////

    // function: select objects by type (annotation or cell detection) and class (e.g. microglia), then rename with prefix and suffix.
    def renameObjects (String objectType, String className, String objectPrefix) {
        
        def targetObjType = []
        
        // extract the target objects with the defined class 
        if (objectType == "annotation") {targetObjType = getAnnotationObjects().findAll{it.getPathClass() == getPathClass(className)}}
        else if (objectType == "cell") {targetObjType = getDetectionObjects().findAll{it.getPathClass() == getPathClass(className)}}
        else { println "Please define the object type as either annotation or cell."}
             
            println targetObjType.size()
            
            if (targetObjType.size() >1) {
            // Rename each individual target object, start from 1 to N with variable naming convention 
            int counter = 1
                targetObjType.each { it ->
                    // rename each object (anno/cell) with suffix(number) and/or prefix(string) 
                    it.setName(objectPrefix + counter)
                    it.setLocked(true)
                    counter++
                } 
            } else {
                targetObjType.each { it ->
                    // rename object (anno/cell) with prefix(string) only
                    it.setName(objectPrefix + "1")
                    it.setLocked(true)
                }
            }
    }
        