mavenBuild {
  nightlyTests = 'true'
  sonarAnalysis = 'skip'
  nightlySonar = 'true'
  extraBuildProfiles = '-Pdeptrack'
  dependencyCheck = 'enabled'
  dependencyCheckBomName = 'SubscriptionOperationServices'
  extraDeployProfiles = '-Pdocker'
  javadocsDirectories = ['services/target']
  buildAgentLabel = 'build-jdk17'
}
