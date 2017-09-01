import static java.lang.System.err

def inputFiles = ["medical-cloud/medical-cloud.thingml", "medical-gateway/medical-gateway.thingml", "medical-device/medical-device.thingml"]
def filesToDiversify = ["medical-gateway.thingml", "medical-device.thingml"]
def outDir = new File("diverseiot-gen")

// Clear output directory
outDir.deleteDir()

// Flatten the input files
def flatDir = new File(outDir, "flattened")
flatDir.mkdirs()

def writeWithImports(flat, original) {
  for (line in original.readLines()) {
    def match = line =~ /^import \"(.*)\"$/
    if (match.matches()) {
      // Put the file directly in here (NO CYCLIC IMPORTS!)
      def imported = new File(original.parentFile, match.group(1))
      writeWithImports(flat, imported)
    } else {
      flat << line + "\n"
    }
  }
}

for (inputFile in inputFiles) {
  def file = new File(inputFile)
  def flat = new File(flatDir, file.name)
  flat.createNewFile()
  writeWithImports(flat, file)
}

// Generate different versions of the code based on diversification points
def findDiversificationPoints(flat) {
  def reader = new FileReader(flat)
  def lineNo = 0
  def points = [:]
  
  while (true) {
    def line = null;
    lineNo++

    // Find diversification point start
    while ((line = reader.readLine()) != null && !line.trim().startsWith("//DIVERSIFICATION")) { lineNo++ }
    if (line != null) {
      def spec = line.trim() =~ /^\/\/DIVERSIFICATION (\d+)( "(.*)"$)?/
      if (spec.matches()) {
        // Found one
	def id = spec.group(1).toInteger()
	def description = spec.group(3)
	def startNo = lineNo

	// Get the source code alternatives
	def options = []

	// Default source code
	line = reader.readLine()
	lineNo++
	if (line != null)
	  options << line.trim()
	
	// Alternative source codes
	while ((line = reader.readLine()) != null && line.trim().startsWith("//DIVERSIFICATION")) {
	  options << line.trim().substring(17).trim()
	  lineNo++
        }
	
	// Add it to the set of found diversifications
	if (!(id in points)) points[id] = [description: null, variations:[]]
	def point = points[id]
	if (point.description == null && description != null) point.description = description
	point.variations << [start:startNo, end:lineNo, alternatives: options]

	lineNo++
      }
    }

    // End-of-file
    if (line == null) break
  }

  return points
}

def foundError = false
def diversity = [:]
for (fileToDiversify in filesToDiversify) {
  def file = new File(flatDir, fileToDiversify)
  def diversificationPoints = findDiversificationPoints(file)

  for (e in diversificationPoints) {
    // Check that each of the variations of the diversification points is the same length
    def variations = e.value.variations
    if (variations.size > 1) {
      def firstVar = variations[0]
      for (var in variations.tail()) {
        if (var.alternatives.size != firstVar.alternatives.size) {
	  err.print("ERROR - in file ${fileToDiversify}: ")
	  err.print("Diversification ${e.key} \"${e.value.description}\" first defined in lines ${firstVar.start}-${firstVar.end} ")
	  err.print("has ${firstVar.alternatives.size} alternatives, ")
	  err.println("but ${var.alternatives.size} alternatives in lines ${var.start}-${var.end}")
	  foundError = true
	}
      }
    }

    // Add to list of diversifications
    if (!(e.key in diversity)) diversity[e.key] = [description: e.value.description, files: []]
    diversity[e.key].files << [file: file, variations: variations]
  }
}

// Check that the variations has the same lengths across files
for (e in diversity) {
  if (e.value.files.size > 1) {
    def firstFile = e.value.files[0]
    for (file in e.value.files.tail()) {
      if (firstFile.variations[0].alternatives.size != file.variations[0].alternatives.size) {
        err.print("ERROR - in file ${file.file.name}: ")
	err.print("Diversification ${e.key} \"${e.value.description}\" first defined in file ${firstFile.file.name} ")
	err.print("has ${firstFile.variations[0].alternatives.size} alternatives, ")
	err.println("but ${file.variations[0].alternatives.size} alternatives in this file")

        foundError = true
      }
    }
  }
}

if (foundError) {
  err.println("Diversified code will not be generated")
  System.exit(1)
}

// Report how many diversification points we found, and the resulting number of configurations
println("----- REPORT -----")
println("Found ${diversity.size()} diversification points:")
def prodResult = 1
for (e in diversity) {
  def alternatives = e.value.files[0].variations[0].alternatives.size
  prodResult *= alternatives
  println(" ${e.key}. \"${e.value.description}\" with ${alternatives} alternatives")
}
println("Resulting in a total of ${prodResult} possible configurations")

// Generate all combinations of the alternatives
def buildCombinations(list) {
  // Build recursively
  if (list.size == 0) {
    return []
  } else {
    def result = []
    0.upto(list[0].value.files[0].variations[0].alternatives.size-1, { i -> 
      def subList = buildCombinations(list.tail())
      if (subList.size == 0) {
        def ent = [:]
        ent[list[0].key] = i
        result << ent
      } else {
        for (subEnt in subList) {
	  subEnt[list[0].key] = i
	  result << subEnt
	}
      }
    })
    return result
  }
}
def allCombinations = buildCombinations(diversity.entrySet().asList().reverse())

// Generate the diversified source code
def allFiles = diversity.collect({ e -> e.value.files }).flatten().collect({ f -> f.file }).unique()
def divDir = new File(outDir, "diversified")
for (combination in allCombinations) {
  def name = combination.values().asList().join("-")
  def combDir = new File(divDir, name)
  combDir.mkdirs()
}
