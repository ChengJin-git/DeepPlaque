/* Nearest Neighbour Distance (NND) from object A to closest object B, for spatial analysis. 
*   For each object A, find the nearest object B by promixity, calculated and compared by the shortest distance between A and B
*   to identify the A-B pairs as a list of the nearest neighbour object B for each object A  
*   Shortest distance is defined as the Euclidean distance between each centroid XY of object A and B, using the Pythagorean theorem
*   --> create a new line annotation that associates each object A centroid XY with the nearest object B centroid XY
*   further visualization can create a spatial map showing all pairs of object A and nearest B centroids 
*/

// Import QuPath's dependencies of relevance
import qupath.lib.scripting.QP
import qupath.lib.objects.PathObject
import qupath.lib.roi.ROIs
import qupath.lib.roi.LineROI
import qupath.lib.roi.RoiTools.CombineOp
import qupath.lib.roi.GeometryROI
import qupath.opencv.ops.ImageOps.Core
import java.util.ArrayList
import qupath.lib.measurements.MeasurementList

/***************************************************************************************************************************/

    // Different combinations of nearest B from A: the primary object is always A, then find the nearest B from A: 
    // logic = For each A object, calculate distances between A and all other B objects, find the A-B pair with the shortest distance as nearest neighbours.
    // Find the nearest B annotation from each A cell: e.g. def object A = Iba1+ DAPI detections as microglia; def object B = abeta plaque annotations  
    // Find the nearest B cell from each A cell: e.g. def object A = Iba1+ DAPI detections as microglia; def object B = NeuN+ DAPI detections as neurons
    // Find the nearest B annotation from each A annotation: e.g. def object A = abeta plaque annotations; def object B = CLDN5+ BV annotations

    /* (optional pre-processing) If "true", remove existing NND line annotations, otherwise "false" */
    def deleteExistingNNDannotations = true
    
        /* (optional pre-processing) Remove existing line annotations before adding the updated set of line annotataions */
        if (deleteExistingNNDannotations = true) {        
            def NNDlineAnnotations = getAnnotationObjects().findAll{ it.getROI().isLine() && it.getName().matches("NND(.*)")}
                if (NNDlineAnnotations.size() != []) { 
                    removeObjects(NNDlineAnnotations, false)
                } else { println "no line annotations yet."}
            }

    /* Set maximum limit of the length between nearest neighbour object pairs? */
    // boolean maxLimit = true
    // Double maxNNDlength = 100.0

    /* Define object type of A and B , their PathClass (exact class name), and representative object names */
             
        computeNND_addLineAnno("cell", "Annotation", "P2RY12", "abeta", "P2RY12", "abeta", "NND_P2RY12_microglia")
        computeNND_addLineAnno("cell", "Annotation", "P2RY12: Iba1", "abeta", "P2RY12+Iba1", "abeta", "NND_P2RY12+Iba1_microglia")
        computeNND_addLineAnno("cell", "Annotation", "P2RY12: CD74: Iba1", "abeta", "P2RY12+CD74+Iba1", "abeta", "NND_P2RY12+CD74+Iba1_microglia")
        computeNND_addLineAnno("cell", "Annotation", "CD74: Iba1", "abeta", "CD74+Iba1", "abeta", "NND_CD74+Iba1_microglia")
        computeNND_addLineAnno("cell", "Annotation", "Iba1", "abeta", "Iba1", "abeta", "NND_Iba1_microglia")
        //computeNND_addLineAnno("cell", "cell", "Iba-1 (Opal 620)", "NeuN (Opal 690)", "microglia", "neuron", "NND_Iba1-NeuN")
        // computeNND_addLineAnno("Annotation", "Annotation", "abeta", "CLDN5+BV", "abeta", "BV", "NND_plaque-BV")

/***************************************************************************************************************************/

/* function: compute NND from each object A to the closest B, and add as line annoatation */ 
def computeNND_addLineAnno (String objectAtype, String objectBtype, String objectAclass, String objectBclass, String objectAname, String objectBname, String objectABlineClass) {

    // Based on user-input that defined object A and B's type (anno/cell) and class, those list of objects would be extracted accordingly.
    // each list contains single or multiple objects with user-defined object type and class 
    // e.g. Annotations with class "Abeta" can have multiple entities; exisiting cell detections of specific cell type usually have more than one.    
    def objectAlist = []
    def objectBlist = []
    

        if (objectAtype == "cell") { objectAlist = getDetectionObjects().findAll{it.isDetection() && it.getPathClass() == getPathClass(objectAclass)}}
        else if (objectAtype == "Annotation") { objectAlist = getAnnotationObjects().findAll{it.isAnnotation() && it.getPathClass() == getPathClass(objectAclass)} }
    
        if (objectBtype == "cell") { objectBlist = getDetectionObjects().findAll{it.isDetection() && it.getPathClass() == getPathClass(objectBclass)}}
        else if (objectBtype == "Annotation") { objectBlist = getAnnotationObjects().findAll{it.isAnnotation() && it.getPathClass() == getPathClass(objectBclass)} }    
    
    // extract the centroids pxl of the anno and detections as a list of Point2D objects (XY coordinate space)
        // get the list of "Point2D" objects (XY coordinate space) for specified A and B
        // Each inner list contains two elements: the X and Y coordinates, but the size is the same no. of objects in the list
            def objectACentroids = objectAlist.collect { roi ->
                [roi.getROI().getCentroidX(), roi.getROI().getCentroidY()]
            }

            def objectBCentroids = objectBlist.collect { roi ->
                [roi.getROI().getCentroidX(), roi.getROI().getCentroidY()]
            }
                
                println "No. of object A = ${objectACentroids.size()}"
                println "No. of object B = ${objectBCentroids.size()}"
           
    // Create a new list to store the nearest neighbour object B's XY coordinates for each object A
    def nearestAnnotationCoordinates = new ArrayList<Double[]>()
       
        // boolean maxLimit = false
        // Double maxNNDlength = 100.0
        // def minDistance = Double.MAX_VALUE

        // Nested loop: for each object A, find the nearest object B by promixity
        objectACentroids.eachWithIndex { objA, i ->
            
            // ensure that the initial value of minDistance is larger than any possible distance between two objects.
            // set the initial min D as the maximum value possible, hence any 1st distance computed would be counted as nearest distance of the combo.
            // if (maxLimit == true) {
            //     minDistance = maxNNDlength}
            def minDistance = Double.MAX_VALUE             
            def nearestAnnotationIndex = null                
            def nearestAnnotationCoordi = null

            // iteration: looping through each object B to update any smallest value for any possible distance between this detection and all annotations. 
            objectBCentroids.eachWithIndex { objB, j ->
            
                // equation: distance = sqrt((objA[centroidX] - objB[centroidX])^2 + (objA[centroidY] - objB[centroidY])^2)
                 def distance = Math.sqrt(Math.pow(objA[0] - objB[0], 2) + Math.pow(objA[1] - objB[1], 2))
                 
                // if distance between objA and objB is less than the minDistance, then store this pair as nearest coordinates
                if (distance < minDistance) {
                    minDistance = distance
                    nearestAnnotationIndex = j
                    nearestAnnotationCoordi = [objB[0], objB[1]] 
                }
            }
                
                // Add nearest annotation coordinates to list
                nearestAnnotationCoordinates.add(nearestAnnotationCoordi) 
                
    
                    // create empty arrays to hold values 
                    def lineAnnotations = [:]
                    def objectAwithNND = []
                    def nearestAnnotation = []
                    
                    
                
                        if (objectAtype == "cell") { objectAwithNND = getDetectionObjects().find { it.getROI().getCentroidX() == objA[0] && it.getROI().getCentroidY() == objA[1]}}
                        else if (objectAtype == "Annotation") { objectAwithNND = getAnnotationObjects().find { it.getROI().getCentroidX() == objA[0] && it.getROI().getCentroidY() == objA[1]}}
                        if (objectBtype == "cell") { nearestAnnotation = getDetectionObjects().find { it.getPathClass() == getPathClass(objectBclass) && it.getROI().getCentroidX() == nearestAnnotationCoordi[0] && it.getROI().getCentroidY() == nearestAnnotationCoordi[1]}}
                        else if (objectBtype == "Annotation") { nearestAnnotation = getAnnotationObjects().find { it.getPathClass() == getPathClass(objectBclass) && it.getROI().getCentroidX() == nearestAnnotationCoordi[0] && it.getROI().getCentroidY() == nearestAnnotationCoordi[1]}} 


                    
                        def plane = getCurrentViewer()?.getImagePlane()
                            def lineROI = ROIs.createLineROI(objA[0], objA[1], nearestAnnotationCoordi[0], nearestAnnotationCoordi[1], plane)
                                println lineROI.getLength()
                                
                            def NNDline = PathObjects.createAnnotationObject(lineROI)
                            
                            NNDline.setName("NND from ${objectAwithNND.getName()} to ${nearestAnnotation.getName()}")
                            NNDline.setLocked(true)
                            NNDline.setPathClass(getPathClass(objectABlineClass))
                            
                        addObject(NNDline)
                          
                    fireHierarchyUpdate()
                } 
                    
       }

    resetSelection()