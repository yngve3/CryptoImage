package cryptography

import java.io.File
import java.io.FileNotFoundException
import javax.imageio.ImageIO
import kotlin.experimental.xor

fun main() {
    while (true) {
        println("Task (hide, show, exit):")
        when (val input = readln()) {
            "exit" -> {
                exit()
                break
            }
            "hide" -> hide()
            "show" -> show()
            else -> error(input)
        }
    }
}

fun hide() {
    println("Input image file:")
    val input = readln()
    println("Output image file:")
    val output = readln()
    println("Message to hide:")
    val message = readln().encodeToByteArray()
    println("Password:")
    val password = readln().encodeToByteArray()

    val encryptedMessage = xor(message, password) + byteArrayOf(0, 0, 3)

    val result = messageToImage(encryptedMessage, input, output)
    if (result.isSuccess) {
        println(result.getOrNull())
    } else {
        println(result.exceptionOrNull()?.message)
    }
}

fun show() {
    println("Input image file:")
    val file = readln()
    println("Password:")
    val password = readln().encodeToByteArray()

    val result = messageFromImage(file)
    if (result.isSuccess) {
        println(result.getOrNull()?.let { xor(it, password).decodeToString() })
    } else {
        println(result.exceptionOrNull()?.message)
    }
}

fun xor(byteArray1: ByteArray, byteArray2: ByteArray): ByteArray {
    val result = ByteArray(byteArray1.size)
    for (i in byteArray1.indices) {
        result[i] = byteArray1[i] xor byteArray2[i % byteArray2.size]
    }

    return result
}

fun messageToImage(message: ByteArray, imageFileNameIn: String, imageFileNameOut: String): Result<String> {
    val inFile = File(imageFileNameIn)
    if (inFile.exists()) {
        val image = ImageIO.read(inFile)

        if ((message.size * 8) > image.width * image.height) {
            return Result.failure(Exception("The input image is not large enough to hold this message."))
        }

        var x = 0
        var y = 0
        for (i in message) {
            var byte = i.toInt()
            for (j in 0 until  8) {
                val bit = (byte and 0b10000000) shr 7
                val mask = 0b11111111_11111111_11111111_11111110.toInt()
                image.setRGB(x, y, (image.getRGB(x, y) and mask) or bit)

                byte = byte shl 1

                if (x < image.width) {
                    x++
                } else {
                    x = 0
                    y++
                }
            }
        }

        val outFile = File(imageFileNameOut)
        ImageIO.write(image, "png", outFile)
        return Result.success("Message saved in $imageFileNameOut image.")
    } else {
        return Result.failure(FileNotFoundException("Can't read input file!"))
    }
}

fun messageFromImage(imageFileName: String): Result<ByteArray> {
    val file = File(imageFileName)
    if (file.exists()) {
        var encryptedMessage = ByteArray(0)
        val image = ImageIO.read(file)

        var x = 0
        var y = 0
        while (true) {
            var byte = 0
            for (i in 0 until 8) {
                val bit = image.getRGB(x, y) and 1
                byte = byte or bit
                byte = byte shl 1

                if (x < image.width) {
                    x++
                } else {
                    x = 0
                    y++
                }
            }

            encryptedMessage += (byte shr 1).toByte()

            if (encryptedMessage.size > 3) {
                if ((encryptedMessage[encryptedMessage.size - 1].toInt() == 3)
                    and (encryptedMessage[encryptedMessage.size - 2].toInt() == 0)
                    and (encryptedMessage[encryptedMessage.size - 3].toInt() == 0)
                ) {
                    break
                }
            }
        }

        return Result.success(encryptedMessage.dropLast(3).toByteArray())
    } else {
        return Result.failure(FileNotFoundException("Can't read input file!"))
    }
}

fun exit() {
    println("Bye!")
}

fun error(input: String) {
    println("Wrong task: $input")
}


