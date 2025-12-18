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


  
        // Define the distance threshold and the distance reference (boundary = perimeter; centroid = center of plaque) for removing annotations
        def mergeDistanceMicron = 2.0
        def DistanceReference = "boundary"
        
            def cal = getCurrentImageData().getServer().getPixelCalibration()
            double mergeDistancePxl = mergeDistanceMicron / cal.getAveragedPixelSize()
        
        // extract all the target boundary annotations based on their classess
        def anno_others = getAnnotationObjects().findAll{it.getPathClass() == getPathClass("abeta_others")}
        def anno_DL = getAnnotationObjects().findAll{it.getPathClass() == getPathClass("abeta_DL")}
        
            println anno_others.size()    // get the no. of anno_others 
            println anno_DL.size()    // get the no. of anno_DL 
            
            // nested loop to compute distance between each anno pair, and only remove if (distance <= mergeDistance) 
            for (anno1 in anno_others) {
                
                // create a list of the annotations to remove
                def annotationsToremove = []
                
                // Loop through all other annotations
                for (anno2 in anno_DL) {
                    
                    // Skip the current annotation if it is the same as the first annotation
                    if (anno2 == anno1) {
                        // print "For " + anno1.getName() + ", anno2 is the same as anno1, skipped remove."
                        continue
                    }
                                
                      def anno1roi = anno1.getROI()
                      def anno2roi = anno2.getROI()
                      def DistanceFromROI = DistanceReference
                      
                      // compute the Euclidean distance between two ROIs, from Boundary or Centroid
                      if (DistanceFromROI == "centroid") {
                                                            
                              def distanceFromCentroid = RoiTools.getCentroidDistance(anno1roi, anno2roi)
                              //println("Centroid distance between " + anno1.getName() + " and " + anno2.getName() + " = " + distanceFromCentroid)
                        
                                // Check if the distance is less than the threshold for merging annotations
                                if (distanceFromCentroid <= mergeDistancePxl) {
                                    // Get a list of the annotations to remove
                                    annotationsToremove << anno1.getName()
                                }

                        } else if (DistanceFromROI == "boundary") {
                              
                            def distanceFromBoundary = RoiTools.getBoundaryDistance(anno1roi, anno2roi)
                              //println("Boundary distance between " + anno1.getName() + " and " + anno2.getName() + " = " + distanceFromBoundary)
                        
                                // Check if the distance is less than the threshold for merging annotations
                                if (distanceFromBoundary <= mergeDistancePxl) {
                                    // Get a list of the annotations to merge
                                    annotationsToremove << anno1.getName()
                                }
                              
                          }

                  }

                
                // check if the list of annotationsToMerge is empty, if so, remove those boundary annos as one.
                if (annotationsToremove.isEmpty() == false) {    
                    
                    annotationsToremove << anno1.getName()
                        println "to remove " + anno1.getName()
                    
                        def allAnnotations = getAnnotationObjects()
                        def selectedAnnotations = allAnnotations.findAll { an -> 
                            annotationsToremove.contains(an.getDisplayedName()) && 
                            an.getPathClass() == getPathClass("abeta_others")
                        }
                        
                    selectObjects(selectedAnnotations)
                    // Remove the currently-selected annotations 
                    removeObjects(selectedAnnotations,true)
                    
                }
                
                println "no need to remove " + anno1.getName()
            }
            
        resolveHierarchy()
        fireHierarchyUpdate()

