package dev.nucleusframework.nna.plugin.tasks

import dev.nucleusframework.nna.plugin.analysis.PsiParseWorkAction
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@DisableCachingByDefault(because = "Bridge generation depends on source analysis that is not yet cacheable")
abstract class GenerateNativeBridgesTask : DefaultTask() {

    @get:Inject
    abstract val taskWorkerExecutor: WorkerExecutor

    @get:Inject
    abstract val taskObjectFactory: ObjectFactory

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val taskNativeSources: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val taskCommonSources: ConfigurableFileCollection

    @get:Classpath
    abstract val taskPsiClasspath: ConfigurableFileCollection

    @get:Input
    abstract val taskLibName: Property<String>

    @get:Input
    abstract val taskJvmPackage: Property<String>

    @get:OutputDirectory
    abstract val taskOutputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val taskJvmOutputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val taskJvmResourcesDir: DirectoryProperty

    @TaskAction
    fun generate() {
        taskOutputDir.get().asFile.apply { deleteRecursively(); mkdirs() }
        taskJvmOutputDir.get().asFile.apply { deleteRecursively(); mkdirs() }

        val ktFiles = taskNativeSources.asFileTree.filter { it.extension == "kt" }.files
        if (ktFiles.isEmpty()) {
            logger.lifecycle("kne: No Kotlin sources found, skipping."); return
        }

        val commonKtFiles = taskCommonSources.asFileTree.filter { it.extension == "kt" }.files
        logger.lifecycle("kne: Parsing ${ktFiles.size} native + ${commonKtFiles.size} common source file(s) [PSI]...")

        val pluginJarUrl = PsiParseWorkAction::class.java.protectionDomain?.codeSource?.location
        val pluginJar = taskObjectFactory.fileCollection().apply {
            pluginJarUrl?.let { from(java.io.File(it.toURI())) }
        }

        val workQueue = taskWorkerExecutor.classLoaderIsolation {
            classpath.from(taskPsiClasspath)
            classpath.from(pluginJar)
        }

        workQueue.submit(PsiParseWorkAction::class.java) {
            nativeSourceFiles.from(ktFiles)
            commonSourceFiles.from(commonKtFiles)
            libName.set(taskLibName)
            jvmPackage.set(taskJvmPackage)
            nativeBridgesDir.set(taskOutputDir)
            jvmProxiesDir.set(taskJvmOutputDir)
            jvmResourcesDir.set(taskJvmResourcesDir)
        }
    }
}
