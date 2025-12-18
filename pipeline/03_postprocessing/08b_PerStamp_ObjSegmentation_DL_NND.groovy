//*   Define the pixel thresholder(s) and object classifier(s) *BEFORE* applying them in batch processing using this script. 
//*   Highly recommended to configure the classifiers: assign objects below threshold with class "Unclassified"
//*   To adapt this script, re-define the class construct to classify target objects, as parameters to fit in functions.


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

      /* If user segments annotations, otherwise define with "" */
        // define the pixel thresholder name
        def annoPixelThresholderName = "Abeta" 
        def anno1Class = "abeta_others"
        def anno1Prefix = "abeta_"

        // Define whether annotation shall be split into separate entities or not.
        // If true, split into annotations ; If false, remain as one layer of annotation. 
        boolean splitAnno = true
            // filter off small particles by size (area in microns)
            String minAnnoSize = 300.0
            // fill holes if size falls below defined area
            String fillHolesIflower = 1000.0

 
/*************************************************************************************************/
       
    // Prep: ensure image type is immunofluorescence 
    setImageType('FLUORESCENCE');
        

       /* (optional) Segment annotations */
        if (annoPixelThresholderName != null) {

            // define four parameters: name of existing pixel classifier, minimum size of, maximum size to fill holes, then select new objects.
            createAnnotationsFromPixelClassifier(annoPixelThresholderName, 0.0, 0.0, "SELECT_NEW")
                
                // remove the unclassified annotations (i.e. below threshold) 
                toRemove = [] 
                    getAnnotationObjects().findAll{it.getPathClass() == getPathClass("Unclassified")}.each{  
                        rubbish ->
                            toRemove << rubbish
                    }
                removeObjects(toRemove,true)    
            
            if (splitAnno == true ) {
                // Split the layer of classified annotation into individual annotations (as separate entities) 
                selectObjectsByClassification(anno1Class);
                runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}') 

                // filter plaques by size (um^2) and fill holes if object size fall below defined area (um^2)         
                runPlugin('qupath.lib.plugins.objects.RefineAnnotationsPlugin','{"minFragmentSizeMicrons":'+minAnnoSize+',"maxHoleSizeMicrons":'+fillHolesIflower+'}')
                
                resolveHierarchy()
                fireHierarchyUpdate()
            }   
            
            /* call function "renameObjects" to rename annotations like abeta plaques */
            renameObjects("annotation", anno1Class, anno1Prefix)

        }
                
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

///////////////////////////////////////////////////////////////////////////////////////////////////////

    // add all objects back onto the image : 
    all = getAllObjects().findAll{it.isAnnotation() || it.isDetection()}
    addObjects(all)
    
    // resolve the object hierarchy, so child objects are grouped under respective parent within the hierarchy 
    // the spatial localisation of each object decides what is "above" or "below" it, for proper object hierarchy organisation
    resolveHierarchy()
    fireHierarchyUpdate()
    
    // automatically save the processed ImageData within batch run, in case QuPath crashes. 
    def qupath = getQuPath()
    def project = qupath.getProject()
    def imageData = getCurrentImageData()
    def entry = getProjectEntry()
        entry.saveImageData(imageData)           
        // update the temp project file
        qupath.refreshProject()
        // save the project.qpproj file 
        project.syncChanges()
        
///////////////////////////////////////////////////////////////////////////////////////////////////