/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.microspingboot.examples;

import com.mycompany.microspingboot.anotations.GetMapping;
import com.mycompany.microspingboot.anotations.RequestParam;
import com.mycompany.microspingboot.anotations.RestController;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author juan.parroquiano
 */
@RestController
public class GreetingController {

	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

	@GetMapping("/greeting")
	public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        //public static String greeting() {
		return "Hola "+ name;
        }
}
