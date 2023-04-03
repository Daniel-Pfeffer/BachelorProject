import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import social.xperience.cli.InvariantRegistrar

class CompileTest {
    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun fullTest() {
        val kotlinSource = SourceFile.kotlin(
            "TestClass.kt", """
            package social.xperience
            import java.lang.IllegalArgumentException

            class Test {
                private val x: String = "Test"
                private val y: Long = 23

                // on visit property: z has an annotation as the annotation is not "targeted"
                // kotlin will generate
                @Holds(LongVerification::class)
                private var z: Long = 24

                fun doSomething(){
                    /*for (i in 1..3) {
                        z = 2
                        if (i > 2) {
                            return
                        }
                    }
                    if(z > 23) {
                        return
                    }
                    z+=(y+x.length)*/
                    z = -2
                    println("I did something")
                }
                
                fun doNothing(str: String){
                    println("I do nothing at all "+str)
                }

                override fun toString(): String{
                    return "$"+"x, "+"$"+"y, " + "$"+"z"
                }
            }

            class LongVerification : Verification<Long> {
                override fun verify(toVerify: Long) {
                    if(toVerify < 0){
                        throw IllegalArgumentException()
                    }
                }
            }

            fun callTest(){
                println(Test().toString())
            }
        """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(kotlinSource)
            compilerPluginRegistrars = listOf(InvariantRegistrar())
            useIR = true
            inheritClassPath = true
            languageVersion = "2.0"
        }.compile()
        val clazz = result.classLoader.loadClass("social.xperience.Test")
        val invoked = clazz.constructors.first().newInstance()
        clazz.declaredMethods.single { it.name == "doSomething" }.invoke(invoked)
        result.generatedFiles.forEach {
            println(it.absolutePath)
        }

        Assertions.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }
}