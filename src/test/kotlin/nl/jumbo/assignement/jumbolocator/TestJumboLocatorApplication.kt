package nl.jumbo.assignement.jumbolocator

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<JumboLocatorApplication>().with(TestcontainersConfiguration::class).run(*args)
}
