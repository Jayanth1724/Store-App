package com.store_app.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.store_app.models.Product;
import com.store_app.models.ProductDTO;
import com.store_app.repository.ProductsRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/products")
public class ProductsController {
	
	@Autowired
	private ProductsRepository productsRepo;
	
	@GetMapping("")
	public String showAllProducts(Model model) {
		List<Product> products = productsRepo.findAll();
		model.addAttribute("products", products);
		return "products/index";
	}
	
	@GetMapping("/{id}")
	public String getProductById(@PathVariable("id") Integer id, Model model) {
		
		Optional<Product> product = productsRepo.findById(id);
		
		if (product.isPresent()) {
			model.addAttribute("product", product.get());	// Here we add the product to the model
			return "products/ProductDetails";
		} 
		else {
			return "products/Error";
		}
	}
	
	@GetMapping("/create")
	public String createPage(Model model) {
		//ProductDTO productDto = new ProductDTO();
		model.addAttribute("productDto", new ProductDTO());
		return "products/CreateProduct";
	}
	 
	@PostMapping("/create")
	public String saveProduct(
			@Valid @ModelAttribute("productDto") ProductDTO productDto, BindingResult result) {
		
        // If image file is empty, add an error
		if(productDto.getImageFile().isEmpty()) {
			result.addError(new FieldError("productDto", "imageFile", "The image file is required"));
		}
		
        // Check if there are validation errors
		if (result.hasErrors()) {
			return "products/CreateProduct";	// Return to the same page with errors
		}
		
		//save the image file
		MultipartFile image = productDto.getImageFile();
		String storageFileName = image.getOriginalFilename();
		
		try {
			String uploadFile = "public/images/";	// Path to store the uploaded file
			Path uploadPath = Paths.get(uploadFile);
			
	        // Create directory if it doesn't exist
			if(!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}
			
	        // Save the image to the specified directory
			try(InputStream input = image.getInputStream()){
				Files.copy(input, Paths.get(uploadFile + storageFileName), 
						StandardCopyOption.REPLACE_EXISTING);
			}
		} 
		catch (Exception e) {
			System.out.println("Exception: "+e.getMessage());
		}
		
		Product product = new Product();
		product.setName(productDto.getName());
		product.setBrand(productDto.getBrand());
		product.setCategory(productDto.getCategory());
		product.setPrice(productDto.getPrice());
		product.setDescription(productDto.getDescription());
		product.setImageFileName(storageFileName);
		
		productsRepo.save(product);
		
	    // Redirect to the product list after successful save
		return "redirect:/products";
	}
	
	@GetMapping("/edit")
	public String editPage(Model model, @RequestParam Integer id) {
		
		try {
			Product product = productsRepo.findById(id).get();
			model.addAttribute("product", product);
			
			ProductDTO productDto = new ProductDTO();
			productDto.setName(product.getName());
			productDto.setBrand(product.getBrand());
			productDto.setCategory(product.getCategory());
			productDto.setPrice(product.getPrice());
			productDto.setDescription(product.getDescription());
			
			model.addAttribute("productDto", productDto);
		} 
		catch (Exception e) {
			System.out.println("Exception "+e.getMessage());
			return "redirect:/products";
		}
		
		return "products/EditProduct";
	}
	
	@PostMapping("/edit")
	public String updateProduct(Model model, @RequestParam Integer id,
			@Valid @ModelAttribute ProductDTO productDto, BindingResult result) {
		
		try {
			Product product = productsRepo.findById(id).get();
			model.addAttribute("product", product);
			
	        // Check if there are validation errors
			if (result.hasErrors()) {
				return "products/EditProduct";
			}
			
	        // If the new image is not empty, handle file upload
			if(!productDto.getImageFile().isEmpty()) {
				
				//delete old image if it exists
				String uploadFile = "public/images/";
				Path oldImagePath = Paths.get(uploadFile + product.getImageFileName());
				
				try {
					Files.deleteIfExists(oldImagePath);
				} 
				catch (Exception e) {
					System.out.println("Error deleting old image: "+e.getMessage());
				}
				
				//save the new image file
				MultipartFile image = productDto.getImageFile();
				String newImageFileName = image.getOriginalFilename();
				
				try (InputStream input = image.getInputStream()) {
					Files.copy(input, Paths.get(uploadFile + newImageFileName), 
							StandardCopyOption.REPLACE_EXISTING);
				}
				catch(IOException e) {
					System.out.println("Error saving new image: " + e.getMessage());
	                result.rejectValue("imageFile", "error.imageFile", "Failed to upload the image.");
	                return "products/EditProduct";
				}
				
	            // Update the product with the new image filename
				product.setImageFileName(newImageFileName);
			}
			
	        // Update the other fields of the product
			product.setName(productDto.getName());
			product.setBrand(productDto.getBrand());
			product.setCategory(productDto.getCategory());
			product.setPrice(productDto.getPrice());
			product.setDescription(productDto.getDescription());
			
			productsRepo.save(product);
		} 
		catch (Exception e) {
			System.out.println("Error updating product: "+e.getMessage());
		}
		
	    // Redirect to the products list page after successful update
		return "redirect:/products";
	}
	
	@GetMapping("/delete")
	public String deleteProduct(@RequestParam Integer id) {
		
		try {
			Product product = productsRepo.findById(id).get();
			
			// delete product image from the folder 
			Path imagePath = Paths.get("public/images/" + product.getImageFileName());
			
			try {
				Files.delete(imagePath);
			}
			catch (Exception e) {
				System.out.println("Exception: "+e.getMessage());
			}
			
			// delete the product
			productsRepo.delete(product);
		} 
		catch (Exception e) {
			System.out.println("Exception: "+e.getMessage());
		}
		return "redirect:/products";
	}
}
