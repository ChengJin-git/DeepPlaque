/* batch process to add metadata key and respective value, for sorting the project entries 
 *         *** --> sort the project entries by any other metadata keys ***
 * Each image is referred to as a project entry.
 *     Add metadata : (1) ID (2) Num annotations 
 *     --> Sort imageList by ID
 *     --> Sort imageList by Num annotations 
 */

// Default imports of QuPath's dependencies as the access to all the static methods in QPEx and QP directly
// These default bindings are used extensively by QuPath for batch processing
import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.scripting.QP
import qupath.lib.gui.commands.Commands
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.images.servers.ImageServer
import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.projects.ProjectImageEntry
import qupath.lib.gui.measure.ObservableMeasurementTableData

    def project = getProject()
    def fileNames = project.getImageList()
            
        totalNumPlaques = []
            
        // loop through each "entry" as images from ImageList in project
        // Add ID in each image metadata for entries sorting 
        for (entry in fileNames) {
            
            // (optional) clear all metadata 
            //entry.clearMetadata()
            
            // (1) extract ID name from Image filename --> search by index for values of key "ID"
            def imgName = entry.getImageName()
            def fileLength = imgName.length()
            
                // extract the sample ID as substring from filename
                int sampleIndexSTART = imgName.indexOf('[', 0)
                int sampleIndexEND = imgName.indexOf(']', 0)
                int NumIndexSampleID = sampleIndexEND-sampleIndexSTART
                def sampleID = imgName.substring(sampleIndexSTART,sampleIndexEND+1)
            
            // extract ID of image entry as substring from filename
            if (imgName.contains("unstained")) {
                
                int IDindex = 0
                int IDindexEND = imgName.indexOf('unstained', 0)
                def imageID = imgName.substring(IDindex,IDindexEND-1)
                println "unstained stamp from " + imageID
             
                } else {
                    
                    int IDindex = 0
                    
                    if (NumIndexSampleID == 12) {
                        // logic = if ID has 4 digits, then fileLength == var ; otherwise ID with 3 digits
                        if (fileLength == 59) {imageID = imgName.substring(IDindex,IDindex+4)}
                        else {imageID = imgName.substring(IDindex,IDindex+3)}
                    } else {
                        // logic = if ID has 4 digits, then fileLength == var ; otherwise ID with 3 digits
                        if (fileLength == 58) {imageID = imgName.substring(IDindex,IDindex+4)}
                        else {imageID = imgName.substring(IDindex,IDindex+3)}
                    }
                }
                
                def ID_sample = imageID + "_" + sampleID
                println ID_sample
                    
            // (2) Add/update metadata : key = ID, value = BBN no. extracted as substring of ImgName
                //if (binding.hasVariable('ID')) { entry.putMetadataValue('ID', ID_sample) } else {println "no ID"}
            entry.putMetadataValue('ID', imageID)
            entry.putMetadataValue('ID-sample', ID_sample)
            
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            
            // (3) Add/update metadata : key = 'String', value = (NEED TO BE String BUT NOT int / double) 
                // Extract all and count Num of annotation objects from within the hierarchy of entry --> key "Num annotations"
                    String nAnnotations = entry.readHierarchy().getAnnotationObjects().size()
                        
                    def CompactPlaques = entry.readHierarchy().getAnnotationObjects().findAll{it.getPathClass() == getPathClass("compact")}
                    String NumCompact = CompactPlaques.size()
                    def FilaPlaques = entry.readHierarchy().getAnnotationObjects().findAll{it.getPathClass() == getPathClass("fila")}
                    String NumFila = FilaPlaques.size()
                    def DiffusePlaques = entry.readHierarchy().getAnnotationObjects().findAll{it.getPathClass() == getPathClass("diffuse")}
                    String NumDiffuse = DiffusePlaques.size()
                    
                        if (NumCompact != null) { entry.putMetadataValue('N compact', NumCompact)}
                        if (NumFila != null) { entry.putMetadataValue('N fila', NumFila)}
                        if (NumDiffuse != null) { entry.putMetadataValue('N diffuse', NumDiffuse)}
    
            // (LAST step) refresh project GUI after imageList sort by ID
            getQuPath().refreshProject()

        }