
// Export measurements of detections based on the annotation class

import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.PathDetectionObject

// Get the list of all images in the current project
def project = getProject()
def imagesToExport = project.getImageList()

// Separate each measurement value in the output file with a tab ("\t") for .tsv, "," for .csv
def separator = ","

// Choose the columns that will be included in the export
// Note: if 'columnsToInclude' is empty, all columns will be included
def columnsToInclude = new String[]{"Image", "Name", "Classification", "Parent", "Centroid X µm", "Centroid Y µm", "Nucleus: DAPI mean", 
                                    "Nucleus: IBA1 (Opal 690) mean", "Nucleus: P2RY12 (Opal 520) mean", "abeta nearest to microglia", "NND_ED of microglia to abeta", 
                                    "abeta nearest to P2RY12+Iba1+microglia", "NND_ED of P2RY12+Iba1+microglia to abeta"
                                    }

// Choose the type of objects that the export will process
// Other possibilities include:
//    1. PathAnnotationObject
//    2. PathDetectionObject
//    3. PathRootObject
// Note: import statements should then be modified accordingly
def exportType = PathDetectionObject.class

// Choose your *full* output path
def outputPath = "/path/to/output/detection_byAnnoClass.csv"  // TODO: Set your output CSV path

def outputFile = new File(outputPath)

// Create the measurementExporter and start the export
def exporter  = new MeasurementExporter()
                  .imageList(imagesToExport)            // Images from which measurements will be exported
                  .separator(separator)                 // Character that separates values
                  .includeOnlyColumns(columnsToInclude) // Columns are case-sensitive
                  .exportType(exportType)               // Type of objects to export
                  .filter( o -> o.getParent().getPathClass() == getPathClass("Outer_75") )// Keep only objects under the hierarchy of expansion radius of 75 um from plaque perimeter
                  .exportMeasurements(outputFile)        // Start the export process

print "Done!"

