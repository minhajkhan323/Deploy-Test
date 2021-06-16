package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;



@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	// Get User Details
	// Get the user details using username(email)
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;

	//Extract username for all handler
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();

		System.out.println("USERNAME " + userName);

		// fetching user details
		User user = userRepository.getUserByUserName(userName);

		System.out.println("USER " + user);

		// send user details to private dashboard after login
		model.addAttribute("user", user);

	}
	
	//dashboard home handler
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {

		model.addAttribute("title", "Home");
		return "normal/user_dashboard";
	}
	
	

	// Add User form handler - after login successfully
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}
	
	//Process Add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file,
			Principal principal,HttpSession session)
	{
		try 
		{
			String name=principal.getName();
			User user=this.userRepository.getUserByUserName(name);
			
			//Processing and uploading a file
			if(file.isEmpty())
			{
				System.out.println("File Is Empty....");
				contact.setImage("blank_image.png");
			}
			else {
				//upload the file to folder and update the name
				contact.setImage(file.getOriginalFilename());
				
				//path to upload image
				File saveFile=new ClassPathResource("/static/img").getFile(); 
				
				//Creating path for file to upload
				Path path= Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				System.out.println("Image Uploaded Successfully");
			}
			
			user.getContacts().add(contact);
			
			contact.setUser(user);
			
			
			this.userRepository.save(user);
			
			System.out.println("DATA: "+contact);
			System.out.println("Contact added successfully");
			
			//Sucess Message for contact added
			session.setAttribute("message", new Message("Your contact is added successfully", "success"));
			
		} 
		catch (Exception e) 
		{
			System.out.println("ERROR "+e.getMessage());
			
			//Error Message for contact not added
			session.setAttribute("message", new Message("Something went wrong !!", "danger"));
		}
		
		return "normal/add_contact_form";
	}
	
	
	//Show All Contact Handler
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model m,Principal principal)
	{
		m.addAttribute("title","View-Contact");
		
		//Fetch User
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		 Pageable pageable= PageRequest.of(page, 5);
		
		//After fetching user get related contact
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(),pageable);
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	
	//Show Specific Contact Details
	 @GetMapping("/contact/{cid}")
	public String showContactDetail(@PathVariable("cid") Integer cid,Model model,Principal principal)
	{
		System.out.println("CID "+cid);
		
		 Optional<Contact> contactOptional=this.contactRepository.findById(cid);
		 Contact contact = contactOptional.get();
		 
		 String userName = principal.getName();
		 User user= this.userRepository.getUserByUserName(userName);
		 
		 
		 //Authorize login user to fetch his own contacts
			if (user.getId() == contact.getUser().getId())

				model.addAttribute("contact", contact);

			return "normal/contact_detail";
	}
	 
	 
	 
	 //Delete Particular Contact
	 @GetMapping("/delete/{cid}")
	 public String deleteContact(@PathVariable("cid") Integer cid,Model model,Principal principal,HttpSession session)
	 {
		 
		try
		{
			 System.out.println("DELETE-ID "+cid);
			 
			 Optional<Contact> contactOptional = this.contactRepository.findById(cid);
			 Contact contact = contactOptional.get();
			 
			User user =  this.userRepository.getUserByUserName(principal.getName());
			
			//fetch contact image and delete
//			File deleteFile=new ClassPathResource("/static/img").getFile(); 
//			File file2 = new File(deleteFile,contact.getImage());
//			file2.delete();
			
			
			user.getContacts().remove(contact);
			
			this.userRepository.save(user);
			
			 System.out.println("Contact deleted sucussfully");
			 
			 session.setAttribute("message", new Message("Your contact is deleted successfully", "success"));
			 
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		 
		 return "redirect:/user/show-contacts/0";
	 }
	 
	 
	 //Update Contact Details
	 @PostMapping("/update-contact/{cid}")
	 public String updateContact(@PathVariable("cid") Integer cid,Model m)
	 {
		 m.addAttribute("title","Update Contact");
		 
		 Contact contact = this.contactRepository.findById(cid).get();
		 
		 m.addAttribute(contact);
		 
		 return "normal/contact_update";
	 }
	 
	 
	 //Process Update Form
	 @PostMapping("/process-update")
	 public String processUpdate(@ModelAttribute Contact contact,@RequestParam("profileImage") 
	 MultipartFile file,Model m,HttpSession session,Principal principal)
	 {
			try {
				
				//fetch previous contact details for image upload
				Contact previousContact=this.contactRepository.findById(contact.getCid()).get();
				
				if(!file.isEmpty())
				{
					
					//Delete previous pic
					File deleteFile=new ClassPathResource("/static/img").getFile(); 
					File file2 = new File(deleteFile,previousContact.getImage());
					file2.delete();
					
					
					//Update Profile pic
					File saveFile=new ClassPathResource("/static/img").getFile(); 
					Path path= Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
					contact.setImage(file.getOriginalFilename());
				}
				
				else {
					contact.setImage(previousContact.getImage());
				}
				
				User user= this.userRepository.getUserByUserName(principal.getName());
				
				contact.setUser(user);
				
				this.contactRepository.save(contact);
				session.setAttribute("message", new Message("Your Contact is updated successfully...","success"));
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		 
		 System.out.println("CONTACT NAME: "+contact.getName());
		 
		 return "redirect:/user/show-contacts/0";
	 }
	 
	 
	 //Your Profile Handler
	 @GetMapping("/profile")
	 public String myProfile(Model m)
	 {
		
		 m.addAttribute("title","Profile");
		 return "normal/profile";
	 }
	 
	 
	 //open Setting form
	 @GetMapping("/setting")
	 public String profileSetting()
	 {
		 return "normal/settings";
	 }
	 
	 //Change Password handler
	 @PostMapping("/change-password")
	 public String changePassword(@RequestParam("oldPassword")String oldPassword,@RequestParam("newPassword")String newPassword,Principal principal,HttpSession session)
	 {
		 String name  =principal.getName();
		 User currentUser = this.userRepository.getUserByUserName(name);
	
//		 System.out.println("OLD-PASS "+oldPassword);
//		 System.out.println("NEW-PASS "+newPassword);
		 
		 if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword()))
		 {
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword)); 
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("Your password is updated sucessfully","success"));
		 }
		 else 
		 {
			 session.setAttribute("message", new Message("You have entered wrong password! Try again","danger"));
			 return "redirect:/user/setting";
		 }
		 
		 return "redirect:/user/show-contacts/0";
	 }

}
