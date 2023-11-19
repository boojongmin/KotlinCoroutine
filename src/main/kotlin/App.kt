import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random
import kotlin.sequences.take
import kotlin.sequences.toList


inline fun ex(name: String = "", description: String = "", block: () -> Unit) {
    println("----------${name}-${description}---------------------------------------")
    block()
}



suspend fun main() {
    ex("2장 시작") {
        val seq = sequence {
            yield(1)
            yield(2)
            yield(3)

        }

        for (num in seq) {
            println(num)
        }
    }


    ex {
        val seq = sequence {
            println("Generating first")
            yield(1)
            println("Generating second")
            yield(2)
            println("Generating third")
            yield(3)
            println("Done")
        }

        for (num in seq) {
            println("The next number is $num")
        }

        ///////////
        val iterator = seq.iterator()
        println("Starting")
        val first = iterator.next()
        println("First : $first")
        val second = iterator.next()
        println("Second : $second")

        val fibonacci = sequence {
            var first = 0.toBigInteger()
            var second = 1.toBigInteger()
            while (true) {
                yield(first)
                val temp = first
                first += second
                second = temp
            }
        }

// 이런 방식이 더 가독성이 좋아 보임
//        val fibonacci = sequence {
//            var first = 0.toBigInteger()
//            var second = 1.toBigInteger()
//            yield(first)
//            yield(second)
//            while(true) {
//                val next = first + second
//                yield(next)
//                first = second
//                second = next
//            }
//        }

        println(fibonacci.take(10).toList())


        fun randomNumbers(
            seed: Long = System.currentTimeMillis()
        ): Sequence<Int> = sequence {
            val random = Random(seed)
            while (true) {
                yield(random.nextInt())
            }
        }

        fun randomUniqueStrings(
            length: Int,
            seed: Long = System.currentTimeMillis()
        ): Sequence<String> = sequence {
            val random = Random(seed)
            val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            while (true) {
                val randomString = (1..length)
                    .map { i -> random.nextInt(charPool.size) }
                    .map(charPool::get)
                    .joinToString("")
                yield(randomString)
            }
        }.distinct()

        println(randomNumbers().take(10).toList())
        println(randomUniqueStrings(10).take(10).toList())

        for (i in 1..10000) {
            check(randomUniqueStrings(10).take(10).toList().size == 10)
        }
    }

    ex(description = "중단이 있는 경우 sequence를 사용하기보다 다음에 배울 flow를 쓰면 좋다며 미리 안내하는 예제. 지금 당장 소스를 이해할 필요는 없음.") {
        data class User(val name: String)
        class UserApi {
            suspend fun takePage(page: Int): List<User> {
                delay(10) // 중단되는 부분
                return if (page >= 10) {
                    emptyList()
                } else {
                    (1..10).map { User("User ${page * 10 + it}") }
                }
            }
        }

        fun allUsersFlow(api: UserApi): Flow<User> = flow {
            var page = 0
            do {
                val users = api.takePage(page++) // 중단 함수입니다.
                emitAll(users.asFlow())
            } while (users.isNotEmpty())
        }

        println(allUsersFlow(UserApi()).toList())
    }


    ///////////// 3장
    ex("3장 시작") {
        suspend {
            println("Before")

            println("After")
        }()


// !!!!!!!!!!!!! 안멈춤
//        suspend {
//            println("Before")
//
//            suspendCoroutine<Unit> {
//            println("Before too")
// !!!!!!!!!!!!! 안멈춤
//            }
//
//            println("After")
//        }()


        suspend {
            println("Before")

            suspendCoroutine<Unit> {
                println("Before too")
                it.resume(Unit)
//                  아래처럼도 가능
//                it.resumeWith(Result.success(Unit))
                println("Before too2 <- 여기도 실행됨.")
            }
            println("After")
        }()
    }

    /////////////////////

    ex {
        fun continueAfterSecond(continuation: Continuation<Unit>) {
            thread {
                Thread.sleep(10)
                continuation.resume(Unit)
            }
        }

        suspend {
            println("Before")
            suspendCoroutine {
                continueAfterSecond(it)
            }
            println("After")
        }()
    }


    ex {
        val executor = Executors.newSingleThreadScheduledExecutor { Thread(it, "MyThread").apply { isDaemon = true } }

        suspend {
            println("Before")
            suspendCoroutine { continuation ->
                executor.schedule({ continuation.resume(Unit) }, 10, TimeUnit.MILLISECONDS)
            }
            println("After")
        }()
    }


    ex {
        val executor = Executors.newSingleThreadScheduledExecutor { Thread(it, "MyThread").apply { isDaemon = true } }

        suspend fun delay(timeMills: Long) {
            suspendCoroutine { continuation ->
                executor.schedule({ continuation.resume(Unit) }, timeMills, TimeUnit.MILLISECONDS)
            }
        }

        suspend {
            println("Before")
            delay(10)
            println("After")
        }()
    }

    // P29 값으로 재개하기

    ex {
        suspend {
            val i = suspendCoroutine { it.resume(42) }
            println(i)

            val str = suspendCoroutine { it.resume("some text") }
            println(str)

            val b = suspendCoroutine { it.resume(true) }
            println(b)
        }()
    }

    ex(description = "P30. 설명은 좋은데, requestUser 부분은 뭔가 애매") {
        data class User (val name: String)

        fun requestUser(f: (user: User) -> Unit) {
            f(User("User"))
        }

        suspend {
            println("Before")
            val user = suspendCoroutine<User> { continuation ->
                requestUser { user ->
                    continuation.resume(user)
                }
            }

            println(user)
            println("After")

        }()

        // 다음 예제

        // requestUser 함수는 function overloading 되어 있음.
        suspend fun requestUser(): User {
            return suspendCoroutine { continuation ->
                requestUser { user ->
                    continuation.resume(user)
                }
            }
        }

        suspend {
            println("Before")
            val user = requestUser()
            println(user)
            println("After")
        }()

        // 다음 예제
        suspend fun requestUser2(): User {
            return suspendCancellableCoroutine { cont->
                requestUser { user ->
                    cont.resume(user)
                }
            }
        }

        suspend {
            println("Before")
            val user = requestUser2()
            println(user)
            println("After")
        }()
    }

    ex(name="예외로 재개하기", description = "p31") {
        class MyException : Throwable("Just an exception")

        suspend {
            try {
                suspendCoroutine<Unit> { continuation ->
                    continuation.resumeWith(Result.failure(MyException()))
                }
            } catch (e: MyException) {
                println("Caught!")
            }
        }()
    }

    ex {
        // 걍 샘플

        data class User(val name: String)
        data class Response(val isSuccess: Boolean , val user: User? = null, val code: Int, val message: String)
        data class ApiException(val code: Int, override val message: String) : Throwable(message)

        var responses = sequence<Response> {
            yield(Response(true, user = User("User"), 200, "OK"))
            yield(Response(false, code = 500, message = "Internal Server Error"))
        }

        fun requestUser(f: (Response) -> Unit){
            // 이 sequence 사용 방식은 뭔가 깔끔해 보이지 않음. 메서드 하나로 있을법한데 나중에 확인되면 고칠것.
            f(responses.first())
            responses = responses.drop(1)
        }

        suspend fun requestUser(): User {
            return suspendCancellableCoroutine { cont->
                requestUser { resp ->
                    if(resp.isSuccess) {
                        cont.resume(resp.user!!)
                    } else {
                        val e = ApiException(resp.code, resp.message)
                        cont.resumeWithException(e)
                    }
                }
            }
        }

        suspend {
            println("Before")
            println(requestUser())
            try {
                println(requestUser())
            } catch (e: ApiException) {
                println("Caught ${e}")
            }

            try {
                // sequence에서 더이상 drop할게 없어서 나오는 에러 확인용.
                println(requestUser())
            } catch (e: Exception) {
                println("Caught ${e}")
            }
            println("After")
        }()

        // 다른 샘플
        data class News(val title: String)
        fun requestNews(onSuccess: (News) -> Unit, onFailure: (Throwable) -> Unit) { }
        suspend fun requestNews(): News {
            return suspendCancellableCoroutine { cont ->
                requestNews(
                    onSuccess = {news -> cont.resume(news)},
                    onFailure = {e -> cont.resumeWithException(e)}
                )
            }
        }
    }


    ex(name="함수가 아닌 코루틴을 중단시킨다", description = "p33") {
//            // 이렇게 구현하면 안됨!!!
//            var continuation: Continuation<Unit>? = null
//            suspend fun suspendAndSetContinuation() {
//                suspendCoroutine<Unit> {
//                    continuation = it
//                }
//            }
//            suspend {
//                println("Before")
//                suspendAndSetContinuation()
//                continuation?.resume(Unit)
//                println("After")
//            }()
    }

    ex {
        // 이렇게 구현하면 안됨!!! - 메모리 누수
//        var continuation: Continuation<Unit>? = null
//        suspend fun suspendAndSetContinuation() {
//            suspendCoroutine<Unit> {
//                continuation = itimport kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.suspendCancellableCoroutine
//import java.util.concurrent.Executors
//import java.util.concurrent.TimeUnit
//import kotlin.concurrent.thread
//import kotlin.coroutines.Continuation
//import kotlin.coroutines.resume
//import kotlin.coroutines.resumeWithException
//import kotlin.coroutines.suspendCoroutine
//import kotlin.random.Random
//import kotlin.sequences.take
//import kotlin.sequences.toList
//
//
//inline fun ex(name: String = "", description: String = "", block: () -> Unit) {
//    println("----------${name}-${description}---------------------------------------")
//    block()
//}
//
//
//
//suspend fun main() {
//    ex("2장 시작") {
//        val seq = sequence {
//            yield(1)
//            yield(2)
//            yield(3)
//
//        }
//
//        for (num in seq) {
//            println(num)
//        }
//    }
//
//
//    ex {
//        val seq = sequence {
//            println("Generating first")
//            yield(1)
//            println("Generating second")
//            yield(2)
//            println("Generating third")
//            yield(3)
//            println("Done")
//        }
//
//        for (num in seq) {
//            println("The next number is $num")
//        }
//
//        ///////////
//        val iterator = seq.iterator()
//        println("Starting")
//        val first = iterator.next()
//        println("First : $first")
//        val second = iterator.next()
//        println("Second : $second")
//
//        val fibonacci = sequence {
//            var first = 0.toBigInteger()
//            var second = 1.toBigInteger()
//            while (true) {
//                yield(first)
//                val temp = first
//                first += second
//                second = temp
//            }
//        }
//
//// 이런 방식이 더 가독성이 좋아 보임
////        val fibonacci = sequence {
////            var first = 0.toBigInteger()
////            var second = 1.toBigInteger()
////            yield(first)
////            yield(second)
////            while(true) {
////                val next = first + second
////                yield(next)
////                first = second
////                second = next
////            }
////        }
//
//        println(fibonacci.take(10).toList())
//
//
//        fun randomNumbers(
//            seed: Long = System.currentTimeMillis()
//        ): Sequence<Int> = sequence {
//            val random = Random(seed)
//            while (true) {
//                yield(random.nextInt())
//            }
//        }
//
//        fun randomUniqueStrings(
//            length: Int,
//            seed: Long = System.currentTimeMillis()
//        ): Sequence<String> = sequence {
//            val random = Random(seed)
//            val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
//            while (true) {
//                val randomString = (1..length)
//                    .map { i -> random.nextInt(charPool.size) }
//                    .map(charPool::get)
//                    .joinToString("")
//                yield(randomString)
//            }
//        }.distinct()
//
//        println(randomNumbers().take(10).toList())
//        println(randomUniqueStrings(10).take(10).toList())
//
//        for (i in 1..10000) {
//            check(randomUniqueStrings(10).take(10).toList().size == 10)
//        }
//    }
//
//    ex(description = "중단이 있는 경우 sequence를 사용하기보다 다음에 배울 flow를 쓰면 좋다며 미리 안내하는 예제. 지금 당장 소스를 이해할 필요는 없음.") {
//        data class User(val name: String)
//        class UserApi {
//            suspend fun takePage(page: Int): List<User> {
//                delay(10) // 중단되는 부분
//                return if (page >= 10) {
//                    emptyList()
//                } else {
//                    (1..10).map { User("User ${page * 10 + it}") }
//                }
//            }
//        }
//
//        fun allUsersFlow(api: UserApi): Flow<User> = flow {
//            var page = 0
//            do {
//                val users = api.takePage(page++) // 중단 함수입니다.
//                emitAll(users.asFlow())
//            } while (users.isNotEmpty())
//        }
//
//        println(allUsersFlow(UserApi()).toList())
//    }
//
//
//    ///////////// 3장
//    ex("3장 시작") {
//        suspend {
//            println("Before")
//
//            println("After")
//        }()
//
//
//// !!!!!!!!!!!!! 안멈춤
////        suspend {
////            println("Before")
////
////            suspendCoroutine<Unit> {
////            println("Before too")
//// !!!!!!!!!!!!! 안멈춤
////            }
////
////            println("After")
////        }()
//
//
//        suspend {
//            println("Before")
//
//            suspendCoroutine<Unit> {
//                println("Before too")
//                it.resume(Unit)
////                  아래처럼도 가능
////                it.resumeWith(Result.success(Unit))
//                println("Before too2 <- 여기도 실행됨.")
//            }
//            println("After")
//        }()
//    }
//
//    /////////////////////
//
//    ex {
//        fun continueAfterSecond(continuation: Continuation<Unit>) {
//            thread {
//                Thread.sleep(10)
//                continuation.resume(Unit)
//            }
//        }
//
//        suspend {
//            println("Before")
//            suspendCoroutine {
//                continueAfterSecond(it)
//            }
//            println("After")
//        }()
//    }
//
//
//    ex {
//        val executor = Executors.newSingleThreadScheduledExecutor { Thread(it, "MyThread").apply { isDaemon = true } }
//
//        suspend {
//            println("Before")
//            suspendCoroutine { continuation ->
//                executor.schedule({ continuation.resume(Unit) }, 10, TimeUnit.MILLISECONDS)
//            }
//            println("After")
//        }()
//    }
//
//
//    ex {
//        val executor = Executors.newSingleThreadScheduledExecutor { Thread(it, "MyThread").apply { isDaemon = true } }
//
//        suspend fun delay(timeMills: Long) {
//            suspendCoroutine { continuation ->
//                executor.schedule({ continuation.resume(Unit) }, timeMills, TimeUnit.MILLISECONDS)
//            }
//        }
//
//        suspend {
//            println("Before")
//            delay(10)
//            println("After")
//        }()
//    }
//
//    // P29 값으로 재개하기
//
//    ex {
//        suspend {
//            val i = suspendCoroutine { it.resume(42) }
//            println(i)
//
//            val str = suspendCoroutine { it.resume("some text") }
//            println(str)
//
//            val b = suspendCoroutine { it.resume(true) }
//            println(b)
//        }()
//    }
//
//    ex(description = "P30. 설명은 좋은데, requestUser 부분은 뭔가 애매") {
//        data class User (val name: String)
//
//        fun requestUser(f: (user: User) -> Unit) {
//            f(User("User"))
//        }
//
//        suspend {
//            println("Before")
//            val user = suspendCoroutine<User> { continuation ->
//                requestUser { user ->
//                    continuation.resume(user)
//                }
//            }
//
//            println(user)
//            println("After")
//
//        }()
//
//        // 다음 예제
//
//        // requestUser 함수는 function overloading 되어 있음.
//        suspend fun requestUser(): User {
//            return suspendCoroutine { continuation ->
//                requestUser { user ->
//                    continuation.resume(user)
//                }
//            }
//        }
//
//        suspend {
//            println("Before")
//            val user = requestUser()
//            println(user)
//            println("After")
//        }()
//
//        // 다음 예제
//        suspend fun requestUser2(): User {
//            return suspendCancellableCoroutine { cont->
//                requestUser { user ->
//                    cont.resume(user)
//                }
//            }
//        }
//
//        suspend {
//            println("Before")
//            val user = requestUser2()
//            println(user)
//            println("After")
//        }()
//    }
//
//    ex(name="예외로 재개하기", description = "p31") {
//        class MyException : Throwable("Just an exception")
//
//        suspend {
//            try {
//                suspendCoroutine<Unit> { continuation ->
//                    continuation.resumeWith(Result.failure(MyException()))
//                }
//            } catch (e: MyException) {
//                println("Caught!")
//            }
//        }()
//    }
//
//    ex {
//        // 걍 샘플
//
//        data class User(val name: String)
//        data class Response(val isSuccess: Boolean , val user: User? = null, val code: Int, val message: String)
//        data class ApiException(val code: Int, override val message: String) : Throwable(message)
//
//        var responses = sequence<Response> {
//            yield(Response(true, user = User("User"), 200, "OK"))
//            yield(Response(false, code = 500, message = "Internal Server Error"))
//        }
//
//        fun requestUser(f: (Response) -> Unit){
//            // 이 sequence 사용 방식은 뭔가 깔끔해 보이지 않음. 메서드 하나로 있을법한데 나중에 확인되면 고칠것.
//            f(responses.first())
//            responses = responses.drop(1)
//        }
//
//        suspend fun requestUser(): User {
//            return suspendCancellableCoroutine { cont->
//                requestUser { resp ->
//                    if(resp.isSuccess) {
//                        cont.resume(resp.user!!)
//                    } else {
//                        val e = ApiException(resp.code, resp.message)
//                        cont.resumeWithException(e)
//                    }
//                }
//            }
//        }
//
//        suspend {
//            println("Before")
//            println(requestUser())
//            try {
//                println(requestUser())
//            } catch (e: ApiException) {
//                println("Caught ${e}")
//            }
//
//            try {
//                // sequence에서 더이상 drop할게 없어서 나오는 에러 확인용.
//                println(requestUser())
//            } catch (e: Exception) {
//                println("Caught ${e}")
//            }
//            println("After")
//        }()
//
//        // 다른 샘플
//        data class News(val title: String)
//        fun requestNews(onSuccess: (News) -> Unit, onFailure: (Throwable) -> Unit) { }
//        suspend fun requestNews(): News {
//            return suspendCancellableCoroutine { cont ->
//                requestNews(
//                    onSuccess = {news -> cont.resume(news)},
//                    onFailure = {e -> cont.resumeWithException(e)}
//                )
//            }
//        }
//    }
//
//
//    ex(name="함수가 아닌 코루틴을 중단시킨다", description = "p33") {
////            // 이렇게 구현하면 안됨!!!
////            var continuation: Continuation<Unit>? = null
////            suspend fun suspendAndSetContinuation() {
////                suspendCoroutine<Unit> {
////                    continuation = it
////                }
////            }
////            suspend {
////                println("Before")
////                suspendAndSetContinuation()
////                continuation?.resume(Unit)
////                println("After")
////            }()
//    }
//
//    ex {
//        // 이렇게 구현하면 안됨!!! - 메모리 누수
////        var continuation: Continuation<Unit>? = null
////        suspend fun suspendAndSetContinuation() {
////            suspendCoroutine<Unit> {
////                continuation = it
////            }
////        }
////        coroutineScope {
////            println("Before")
////            launch {
////                delay(10)
////                continuation?.resume(Unit)
////            }
////            suspendAndSetContinuation()
////            println("After")
////        }
//    }
//}
//            }
//        }
//        coroutineScope {
//            println("Before")
//            launch {
//                delay(10)
//                continuation?.resume(Unit)
//            }
//            suspendAndSetContinuation()
//            println("After")
//        }
    }
}
