import static java.lang.System.out
import static java.lang.System.err
import static groovy.io.FileType.*

// Definitions for directory traversal
def staticServices = [
  cloud: "medical-cloud",
]

def diversifiedServices = [
  gateway: "medical-gateway",
  device: "medical-device",
]

// Look at all the generated files
def findDockerAndThingmlOrNull(dir, name) {
  def docker = null
  def thingml = null
  dir.eachFileMatch(FILES, ~/Dockerfile.${name}/, { docker = it })
  dir.eachFileMatch(FILES, ~/${name}.thingml/, { thingml = it })
  if (docker != null && thingml != null) {
    return [
      name: name,
      dockerfile: docker,
      thingmlfile: thingml,
    ]
  } else {
    return null
  }
}

def outDir = new File("diverseiot-gen")
def divDir = new File(outDir,"diversified")
for (e in staticServices) {
  def name = e.value
  e.value = findDockerAndThingmlOrNull(divDir, name)
}

def serviceDef = diversifiedServices
diversifiedServices = []
divDir.eachFile(DIRECTORIES, { combDir ->
  def service = [
    id: combDir.name
  ]
  def foundAll = true
  for (e in serviceDef) {
    def res = findDockerAndThingmlOrNull(combDir, e.value)
    if (res != null) service[e.key] = res
    else foundAll = false
  }
  if (foundAll) diversifiedServices << service
})


// Create the docker compose file
def composeFile = new File(outDir, "docker-compose.yml")
composeFile.createNewFile()
composeFile.text = "version: '3'\nservices:\n"

// Include the simple services
composeFile << "  logger:\n"
composeFile << "    image: diverseiot/logger\n"

// Include all diversified services
def combinationId = 0
for (service in diversifiedServices) {
  def id = service.id
  for (c in service) {
    if (c.key != "id") {
      def dockerfile = c.value.dockerfile
      def context = outDir.toPath().relativize(dockerfile.parentFile.toPath())

      composeFile << "  " << c.key << "_" << id << ":\n"
      composeFile << "    image: diverseiot/" << c.key << ":" << id << "\n"
      composeFile << "    build:\n"
      composeFile << "      context: " << context << "\n"
      composeFile << "      dockerfile: " << dockerfile.name << "\n"
      composeFile << "    command: bash -c \"socat pty,raw,echo=0,link=/dev/serial1 tcp:logger:${combinationId+5000},forever & while [ ! -e /dev/serial1 ]; do sleep 1; done; \$\$THINGML_CMD\"\n"
      composeFile << "    links:\n"
      composeFile << "     - logger\n"
      
      // Write the combinationId in the .thingml file
      def thingmlFile = c.value.thingmlfile
      def oldContent = thingmlFile.text
      thingmlFile.text = ""
      oldContent.eachLine({ line ->
        if (line.contains("/*COMBINATIONID*/")) {
          line = line.substring(0,line.indexOf("/*COMBINATIONID*/")) + combinationId
        }
        thingmlFile << line << "\n"
      })
    }
  }
  combinationId++
}

// Include all static services
for (service in staticServices) {
  def dockerfile = service.value.dockerfile
  def context = outDir.toPath().relativize(dockerfile.parentFile.toPath())
      
  composeFile << "  " << service.key << ":\n"
  composeFile << "    image: diverseiot/" << service.key << ":static\n"
  composeFile << "    build:\n"
  composeFile << "      context: " << context << "\n"
  composeFile << "      dockerfile: " << dockerfile.name << "\n"
  composeFile << "    command: bash -c \"\$\$THINGML_CMD\"\n"
  composeFile << "    links:\n"
  composeFile << "     - logger\n"
}