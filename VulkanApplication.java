import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.lwjgl.system.MemoryUtil.NULL;//Nullpointer for Pointers
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.memUTF8;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXTI;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFormatProperties;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkOffset3D;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;//Use Vk11 version?
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import static org.lwjgl.vulkan.EXTBlendOperationAdvanced.VK_ACCESS_COLOR_ATTACHMENT_READ_NONCOHERENT_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.vkCreateDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_API_VERSION_1_1;
import static org.lwjgl.vulkan.VK11.VK_IMAGE_LAYOUT_DEPTH_READ_ONLY_STENCIL_ATTACHMENT_OPTIMAL;

public abstract class VulkanApplication<T extends VulkanApplicationPerFrameThreadObject> {

	protected final Charset defaultVkCharset = Charset.defaultCharset();
	
	protected final PointerBuffer requiredExtensions;
	protected final String[] requestedValidationLayers;
	protected final String[] requestedPhysicalDeviceExtensions;
	protected VkPhysicalDeviceFeatures requestedPhysicalDeviceFeatures;
	protected VkPhysicalDeviceFeatures2 requestedPhysicalDeviceFeaturesExtensionChain;
	
	protected VkInstance instance;
	protected LongBuffer debugMessenger;
	protected VkPhysicalDevice physicalDevice;
	protected VkDevice device;
	protected VkQueue graphicsQueue;
	protected VkQueue presentationQueue;//Maybe set to same as graphicsQueue if possible
	protected VkQueue transferQueue;
	protected VkQueue computeQueue;
	protected final int[] queueFamilyIndices = new int[4];//graphicsQueue at 0, presenationQueue at 1, transferQueue at 2, computeQueue at 3
	protected LongBuffer graphicsCommandPool = memAllocLong(1);
	protected LongBuffer transferCommandPool = memAllocLong(1);
	protected LongBuffer computeCommandPool = memAllocLong(1);
	
	protected T[] perFrameThreadObjects;
	
	public VulkanApplication(PointerBuffer requiredExtensions, String[] requestedValidationLayers, String[] requestedPhysicalDeviceExtensions, VkPhysicalDeviceFeatures requestedPhysicalDeviceFeatures, VkPhysicalDeviceFeatures2 requestedPhysicalDeviceFeaturesExtensionChain) {
		this.requiredExtensions = requiredExtensions;
		this.requestedValidationLayers = requestedValidationLayers;
		this.requestedPhysicalDeviceExtensions = requestedPhysicalDeviceExtensions;
		this.requestedPhysicalDeviceFeatures = requestedPhysicalDeviceFeatures;
		this.requestedPhysicalDeviceFeaturesExtensionChain = requestedPhysicalDeviceFeaturesExtensionChain;
	}
	
	
	
	protected void createInstance(String[] requestedValidationLayers, PointerBuffer requiredExtensions) {	
		if(requestedValidationLayers.length>0) {
			int[] layerCount = new int[1];
			VkLayerProperties.Buffer layers = getSupportedValidationLayers(layerCount);
			for(int i=0;i<requestedValidationLayers.length;++i) {
				boolean currentSupported=false;
				for(int j=0;j<layerCount[0];++j) {
					layers.position(j);
					if(layers.layerNameString().equals(requestedValidationLayers[i])) {//Might cause Problems with not UTF8 Layernames
						currentSupported=true;
						break;
					}
				}
				if(!currentSupported) {
					throw new RuntimeException("Requested ValidationLayer not supported: "+requestedValidationLayers[i]);
				}
			}
			layers.free();

		}
		
		VkApplicationInfo appInfo = VkApplicationInfo.calloc();
		appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
		appInfo.pApplicationName(String2ByteBuffer("Hello Triangle",defaultVkCharset));
		appInfo.applicationVersion(VK_MAKE_VERSION(1,0,0));
		appInfo.pEngineName(String2ByteBuffer("No Engine",defaultVkCharset));
		appInfo.engineVersion(VK_MAKE_VERSION(1,0,0));
		appInfo.apiVersion(VK_API_VERSION_1_1);
		
		VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc();
		instanceCreateInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
		instanceCreateInfo.pApplicationInfo(appInfo);
		
		PointerBuffer requestedExtensions = memAllocPointer(requiredExtensions.limit()+1
				+(requestedPhysicalDeviceFeaturesExtensionChain != null?1:0));
		for(int i=0;i<requiredExtensions.limit();++i) {
			requestedExtensions.put(requiredExtensions.get(i));
		}
		PointerBuffer ppEnabledLayerNames = null;
		ppEnabledLayerNames = memAllocPointer(requestedValidationLayers.length);
		if(requestedValidationLayers.length>0) {
			for(int i=0;i<requestedValidationLayers.length;++i) {
				ppEnabledLayerNames.put(String2ByteBuffer(requestedValidationLayers[i],defaultVkCharset));
			}
			ppEnabledLayerNames.flip();
			instanceCreateInfo.ppEnabledLayerNames(ppEnabledLayerNames);
		}
		requestedExtensions.put(String2ByteBuffer("VK_EXT_debug_utils",defaultVkCharset));
		if(requestedPhysicalDeviceFeaturesExtensionChain != null) {
			requestedExtensions.put(String2ByteBuffer("VK_KHR_get_physical_device_properties2",defaultVkCharset));
		}
		requestedExtensions.flip();
		
		//Check Required Extensions are available
		int[] extensionCount = new int[1];
		VkExtensionProperties.Buffer extensions = getAvailableExtensions(extensionCount);
		for(int i=0;i<requestedExtensions.limit();++i) {
			boolean currentAvailable=false;
			for(int j=0;j<extensionCount[0];++j) {
				extensions.position(j);
				if(requestedExtensions.getStringUTF8(i).equals(extensions.extensionNameString())) {//Might cause Problems with not UTF8 Extensionnames
					currentAvailable=true;
					break;
				}
			}
			if(!currentAvailable) {
				throw new RuntimeException("Requested Extension not available: "+requestedExtensions.getStringUTF8());
			}
		}
		extensions.free();
		
		instanceCreateInfo.ppEnabledExtensionNames(requestedExtensions);
		
		PointerBuffer instancePointer= memAllocPointer(1);
		int err = vkCreateInstance(instanceCreateInfo, null, instancePointer);
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create VkInstance: " + err);
		}
		
		instance= new VkInstance(instancePointer.get(0),instanceCreateInfo);
		
		appInfo.free();
		instanceCreateInfo.free();
		memFree(requestedExtensions);
		if(ppEnabledLayerNames != null) {
			memFree(ppEnabledLayerNames);
		}
	}
	
	protected void setupDebugMessenger(){
		VkDebugUtilsMessengerCreateInfoEXT debugMessengerCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc();
		debugMessengerCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
		debugMessengerCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT|
				VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT|
				VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT|
				VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
		debugMessengerCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT|
				VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT|
				VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
		debugMessengerCreateInfo.pfnUserCallback(new debugCallback());
		debugMessengerCreateInfo.pUserData(NULL);//optional

		debugMessenger = memAllocLong(1);
		int err=vkCreateDebugUtilsMessengerEXT(instance, debugMessengerCreateInfo, null, debugMessenger);

		if(err!=VK_SUCCESS) {
			throw new AssertionError("Failed to create DebugUtilsMessenger: " + err);
		}
		debugMessengerCreateInfo.free();
	}
	
	protected abstract VkPhysicalDevice pickPhysicalDevice();
	
	protected void createLogicalDevice(String[] requestedValidationLayers, String[] requestedPhysicalDeviceExtensions, VkPhysicalDeviceFeatures requestedPhysicalDeviceFeatures, VkPhysicalDeviceFeatures2 requestedPhysicalDeviceFeaturesExtensionChain, HashMap<String, Boolean> supportedPhysicalDeviceExtensions) {
		int uniqueQueueCount = 1;
		//Create graphicsQueue Createinfo
		VkDeviceQueueCreateInfo graphicsQueueCreateInfo = VkDeviceQueueCreateInfo.calloc();
		graphicsQueueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
		graphicsQueueCreateInfo.queueFamilyIndex(queueFamilyIndices[0]);
		
		FloatBuffer graphicsQueuePriorities = memAllocFloat(4);
		graphicsQueuePriorities.put(1.0f);
		
		FloatBuffer presentationQueuePriorities = memAllocFloat(3);
		
		FloatBuffer transferQueuePriorities = memAllocFloat(2);
		
		//Create presentationQueue Createinfo
		VkDeviceQueueCreateInfo presentationQueueCreateInfo = null;
		boolean uniquePresentationQueue = false;
		if(queueFamilyIndices[0]==queueFamilyIndices[1]) {
			graphicsQueuePriorities.put(1.0f);
		}else {
			uniquePresentationQueue = true;
			uniqueQueueCount++;
			presentationQueueCreateInfo = VkDeviceQueueCreateInfo.calloc();
			presentationQueueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
			presentationQueueCreateInfo.queueFamilyIndex(queueFamilyIndices[1]);//Must be unique!!!!!Handle case when both queueindices are the same
			
			presentationQueuePriorities.put(1.0f);
		}
		
		//Create transferQueue createinfo
		VkDeviceQueueCreateInfo transferQueueCreateInfo = null;
		boolean uniqueTransferQueue = false;
		if(queueFamilyIndices[0]==queueFamilyIndices[2]){
			graphicsQueuePriorities.put(1.0f);
		}else if(queueFamilyIndices[1]==queueFamilyIndices[2]){
			presentationQueuePriorities.put(1.0f);
		}else{
			uniqueTransferQueue = true;
			uniqueQueueCount++;
			transferQueueCreateInfo = VkDeviceQueueCreateInfo.calloc();
			transferQueueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
			transferQueueCreateInfo.queueFamilyIndex(queueFamilyIndices[2]);
			
			transferQueuePriorities.put(1.0f);
		}
		

		//Create computeQueue createinfo
		VkDeviceQueueCreateInfo computeQueueCreateInfo = null;
		boolean uniqueComputeQueue = false;
		if(queueFamilyIndices[0]==queueFamilyIndices[3]) {
			graphicsQueuePriorities.put(1.0f);
		}else if(queueFamilyIndices[1]!=queueFamilyIndices[3]) {
			presentationQueuePriorities.put(1.0f);
		}else if(queueFamilyIndices[2]!=queueFamilyIndices[3]){
			transferQueuePriorities.put(1.0f);
		}else {
			uniqueComputeQueue = true;
			uniqueQueueCount++;
			computeQueueCreateInfo = VkDeviceQueueCreateInfo.calloc();
			computeQueueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
			computeQueueCreateInfo.queueFamilyIndex(queueFamilyIndices[3]);
			
			FloatBuffer computeQueuePriority = memAllocFloat(1);
			computeQueuePriority.put(1.0f);
			computeQueuePriority.flip();//!!!!DON'T FORGET TO FLIP BUFFER!!!!!!
			computeQueueCreateInfo.pQueuePriorities(computeQueuePriority);
			memFree(computeQueuePriority);
		}
		
		graphicsQueuePriorities.flip();//!!!!DON'T FORGET TO FLIP BUFFER!!!!!!
		graphicsQueueCreateInfo.pQueuePriorities(graphicsQueuePriorities);
		
		VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueCount);
		queueCreateInfos.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
		//Add all Createinfos
		queueCreateInfos.put(graphicsQueueCreateInfo);
		if(uniquePresentationQueue) {
			presentationQueuePriorities.flip();//!!!!DON'T FORGET TO FLIP BUFFER!!!!!!
			presentationQueueCreateInfo.pQueuePriorities(presentationQueuePriorities);
			queueCreateInfos.put(presentationQueueCreateInfo);
		}
		if(uniqueTransferQueue) {
			transferQueuePriorities.flip();//!!!!DON'T FORGET TO FLIP BUFFER!!!!!!
			transferQueueCreateInfo.pQueuePriorities(transferQueuePriorities);
			queueCreateInfos.put(transferQueueCreateInfo);
		}
		if(uniqueComputeQueue) {
			queueCreateInfos.put(computeQueueCreateInfo);
		}
		queueCreateInfos.flip();//!!!!DON'T FORGET TO FLIP BUFFER!!!!!!
		
		VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc();
		deviceCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
		deviceCreateInfo.pQueueCreateInfos(queueCreateInfos);
		VkDeviceCreateInfo.nqueueCreateInfoCount(deviceCreateInfo.address(), uniqueQueueCount);
		
		int supportedExtensionsCount = 0;
		for(Boolean b: supportedPhysicalDeviceExtensions.values()) {
			if(b.equals(Boolean.TRUE)) {
				supportedExtensionsCount++;
			}
		}
		

		VkDeviceCreateInfo.nenabledExtensionCount(deviceCreateInfo.address(), supportedExtensionsCount);
		PointerBuffer ppEnabledExtensionNames = memAllocPointer(supportedExtensionsCount);
		for(int i=0;i<requestedPhysicalDeviceExtensions.length;++i) {
			if(supportedPhysicalDeviceExtensions.get(requestedPhysicalDeviceExtensions[i]).equals(Boolean.TRUE)) {
				ppEnabledExtensionNames.put(String2ByteBuffer(requestedPhysicalDeviceExtensions[i],defaultVkCharset));
			}
		}
		ppEnabledExtensionNames.flip();
		deviceCreateInfo.ppEnabledExtensionNames(ppEnabledExtensionNames);
		
		if(requestedPhysicalDeviceFeaturesExtensionChain != null) {
			requestedPhysicalDeviceFeaturesExtensionChain.features(requestedPhysicalDeviceFeatures);
			deviceCreateInfo.pNext(requestedPhysicalDeviceFeaturesExtensionChain.address());
		}else {
			deviceCreateInfo.pEnabledFeatures(requestedPhysicalDeviceFeatures);
		}
		
		PointerBuffer devicePointer = memAllocPointer(1);
		int err = vkCreateDevice(physicalDevice, deviceCreateInfo, null, devicePointer);
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create logical Device: " + err);
		}
		
		device = new VkDevice(devicePointer.get(0), physicalDevice, deviceCreateInfo);
		
		int[] inQueueIndices = new int[queueFamilyIndices.length];
		//Get Queuevariables
		PointerBuffer graphicsQueuePointer = memAllocPointer(1);
		vkGetDeviceQueue(device, queueFamilyIndices[0], 0, graphicsQueuePointer);
		graphicsQueue = new VkQueue(graphicsQueuePointer.get(0),device);
		inQueueIndices[0]++;
		
		if(uniquePresentationQueue) {
			PointerBuffer presentationQueuePointer = memAllocPointer(1);
			vkGetDeviceQueue(device, queueFamilyIndices[1], 0, presentationQueuePointer);
			presentationQueue = new VkQueue(graphicsQueuePointer.get(0),device);
			memFree(presentationQueuePointer);
			inQueueIndices[1]++;
		}else {
			PointerBuffer presentationQueuePointer = memAllocPointer(1);
			vkGetDeviceQueue(device, queueFamilyIndices[1], inQueueIndices[0], presentationQueuePointer);
			presentationQueue = new VkQueue(presentationQueuePointer.get(0),device);
			memFree(presentationQueuePointer);
			inQueueIndices[0]++;
		}
		
		if(uniqueTransferQueue) {
			PointerBuffer transferQueuePointer = memAllocPointer(1);
			vkGetDeviceQueue(device, queueFamilyIndices[2], 0, transferQueuePointer);
			transferQueue = new VkQueue(transferQueuePointer.get(0), device);
			memFree(transferQueuePointer);
			inQueueIndices[2]++;
		}else {
			if(queueFamilyIndices[2] == queueFamilyIndices[0]) {
				PointerBuffer transferQueuePointer = memAllocPointer(1);
				vkGetDeviceQueue(device, queueFamilyIndices[2], inQueueIndices[0], transferQueuePointer);
				transferQueue = new VkQueue(transferQueuePointer.get(0), device);
				memFree(transferQueuePointer);
				inQueueIndices[0]++;
			}else {
				PointerBuffer transferQueuePointer = memAllocPointer(1);
				vkGetDeviceQueue(device, queueFamilyIndices[2], inQueueIndices[1], transferQueuePointer);
				transferQueue = new VkQueue(transferQueuePointer.get(0), device);
				memFree(transferQueuePointer);
				inQueueIndices[1]++;
			}
		}
		
		if(uniqueComputeQueue) {
			PointerBuffer computeQueuePointer = memAllocPointer(1);
			vkGetDeviceQueue(device, queueFamilyIndices[3], 0, computeQueuePointer);
			computeQueue = new VkQueue(computeQueuePointer.get(0), device);
			memFree(computeQueuePointer);
			inQueueIndices[3]++;
		}else{
			if(queueFamilyIndices[3] == queueFamilyIndices[0]) {
				PointerBuffer computeQueuePointer = memAllocPointer(1);
				vkGetDeviceQueue(device, queueFamilyIndices[3], inQueueIndices[0], computeQueuePointer);
				computeQueue = new VkQueue(computeQueuePointer.get(0), device);
				memFree(computeQueuePointer);
				inQueueIndices[0]++;
			}else if(queueFamilyIndices[3] == queueFamilyIndices[1]){
				PointerBuffer computeQueuePointer = memAllocPointer(1);
				vkGetDeviceQueue(device, queueFamilyIndices[3], inQueueIndices[1], computeQueuePointer);
				computeQueue = new VkQueue(computeQueuePointer.get(0), device);
				memFree(computeQueuePointer);
				inQueueIndices[1]++;
			}else {
				PointerBuffer computeQueuePointer = memAllocPointer(1);
				vkGetDeviceQueue(device, queueFamilyIndices[3], inQueueIndices[2], computeQueuePointer);
				computeQueue = new VkQueue(computeQueuePointer.get(0), device);
				memFree(computeQueuePointer);
				inQueueIndices[2]++;
			}
		}
		
		memFree(devicePointer);
		memFree(graphicsQueuePriorities);
		memFree(presentationQueuePriorities);
		memFree(transferQueuePriorities);
		memFree(ppEnabledExtensionNames);
		memFree(graphicsQueuePointer);
		graphicsQueueCreateInfo.free();
		if(presentationQueueCreateInfo!=null) {
			presentationQueueCreateInfo.free();
		}
		if(transferQueueCreateInfo!=null) {
			transferQueueCreateInfo.free();
		}
		if(computeQueueCreateInfo!=null) {
			computeQueueCreateInfo.free();
		}
		deviceCreateInfo.free();
		queueCreateInfos.free();
		
	}
	
	protected void createCommandPools() {
		VkCommandPoolCreateInfo graphicsCommandPoolCreateInfo = VkCommandPoolCreateInfo.calloc();
		graphicsCommandPoolCreateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
		graphicsCommandPoolCreateInfo.queueFamilyIndex(queueFamilyIndices[0]);
		graphicsCommandPoolCreateInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
		
		int err = vkCreateCommandPool(device, graphicsCommandPoolCreateInfo, null, graphicsCommandPool);
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create graphicsCommandPool: " + err);
		}
		
		VkCommandPoolCreateInfo transferCommandPoolCreateInfo = VkCommandPoolCreateInfo.calloc();
		transferCommandPoolCreateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
		transferCommandPoolCreateInfo.queueFamilyIndex(queueFamilyIndices[2]);
		transferCommandPoolCreateInfo.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
		
		err = vkCreateCommandPool(device, transferCommandPoolCreateInfo, null, transferCommandPool);
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create transferCommandPool: " + err);
		}
		
		VkCommandPoolCreateInfo computeCommandPoolCreateInfo = VkCommandPoolCreateInfo.calloc();
		computeCommandPoolCreateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
		computeCommandPoolCreateInfo.queueFamilyIndex(queueFamilyIndices[3]);
		computeCommandPoolCreateInfo.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
		
		err = vkCreateCommandPool(device, computeCommandPoolCreateInfo, null, computeCommandPool);
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create computeCommandPool: " + err);
		}
		
		graphicsCommandPoolCreateInfo.free();
		transferCommandPoolCreateInfo.free();
		computeCommandPoolCreateInfo.free();
	}
	
	protected abstract T[] createPerFrameThreadObjects();
	
	protected abstract boolean isPhysicalDeviceSuitable(VkPhysicalDevice physicalDevice);
	
	protected void createBuffer(long size, int usage, int properties, LongBuffer buffer, LongBuffer bufferMemory, int sharingMode, int[] queueFamilyIndices) {
		VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc();
		bufferCreateInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
		bufferCreateInfo.size(size);
		bufferCreateInfo.usage(usage);
		bufferCreateInfo.sharingMode(sharingMode);
		IntBuffer familyIndices = memAllocInt(queueFamilyIndices.length);
		for(int i=0;i<queueFamilyIndices.length;++i) {
			familyIndices.put(queueFamilyIndices[i]);
		}
		familyIndices.flip();
		bufferCreateInfo.pQueueFamilyIndices(familyIndices);
		
		int err = vkCreateBuffer(device, bufferCreateInfo, null, buffer);
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create Buffer: " + err);
		}

		VkMemoryRequirements memoryRequirements = VkMemoryRequirements.calloc();
		vkGetBufferMemoryRequirements(device, buffer.get(0), memoryRequirements);
		
		VkMemoryAllocateInfo memoryAllocateInfo = VkMemoryAllocateInfo.calloc();
		memoryAllocateInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
		memoryAllocateInfo.allocationSize(memoryRequirements.size());
		memoryAllocateInfo.memoryTypeIndex(findMemoryType(memoryRequirements.memoryTypeBits(), 
				properties));
		
		err = vkAllocateMemory(device, memoryAllocateInfo, null, bufferMemory);//Don't do it per Buffer...See VulkanMemoryAllocator..Aliasing
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to allocate Buffer memory: " + err);
		}
		
		vkBindBufferMemory(device, buffer.get(0), bufferMemory.get(0), 0L);
		
		memoryAllocateInfo.free();
		bufferCreateInfo.free();
		memoryRequirements.free();
		memFree(familyIndices);
	}
	
	protected void createImage(int width, int height, int mipLevels, int numSamples, int format, int tiling, int usage, int properties, LongBuffer image, LongBuffer imageMemory, int sharingMode) {
		createImage(width, height, mipLevels, numSamples, format, tiling, usage, properties, image, imageMemory, sharingMode, VK_IMAGE_TYPE_2D, 1);
	}
	
	protected void createImage(int width, int height, int mipLevels, int numSamples, int format, int tiling, int usage, int properties, LongBuffer image, LongBuffer imageMemory, int sharingMode, int imageType, int arrayLayers) {
		VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.calloc();
		imageCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
		imageCreateInfo.imageType(imageType);
		VkExtent3D extent = VkExtent3D.calloc().width(width).height(height).depth(1);
		imageCreateInfo.extent(extent);
		imageCreateInfo.mipLevels(mipLevels);
		imageCreateInfo.arrayLayers(arrayLayers);
		imageCreateInfo.format(format);
		imageCreateInfo.tiling(tiling);
		imageCreateInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
		imageCreateInfo.usage(usage);
		imageCreateInfo.sharingMode(sharingMode);//TODO:Only used in one queueFamily. Create different variables for every usage and determine sharingMode at queueFamilySelection.
		imageCreateInfo.samples(numSamples);
		imageCreateInfo.flags(0);
		
		int err = vkCreateImage(device, imageCreateInfo, null, image);//Textureformat might not be supported!!!
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create Image: " + err);
		}
		
		VkMemoryRequirements memoryRequirements = VkMemoryRequirements.calloc();
		vkGetImageMemoryRequirements(device, image.get(0), memoryRequirements);
		
		VkMemoryAllocateInfo memoryAllocateInfo = VkMemoryAllocateInfo.calloc();
		memoryAllocateInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
		memoryAllocateInfo.allocationSize(memoryRequirements.size());
		memoryAllocateInfo.memoryTypeIndex(findMemoryType(memoryRequirements.memoryTypeBits(), 
				properties));
		
		err = vkAllocateMemory(device, memoryAllocateInfo, null, imageMemory);//Don't do it per Image...See VulkanMemoryAllocator..Aliasing
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to allocate image memory: " + err);
		}
		
		vkBindImageMemory(device, image.get(0), imageMemory.get(0), 0L);
		
		extent.free();
		memoryRequirements.free();
		memoryAllocateInfo.free();
	}
	
	protected long createImageView(long image, int format, int aspectMask, int mipLevels) {
		return createImageView(image, format, aspectMask, mipLevels, 0, 1, VK_IMAGE_VIEW_TYPE_2D);
	}
	
	protected long createImageView(long image, int format, int aspectMask, int mipLevels, int baseArrayLayer, int layerCount, int viewType) {
		VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.calloc();
		imageViewCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
		imageViewCreateInfo.image(image);
		imageViewCreateInfo.viewType(viewType);
		imageViewCreateInfo.format(format);
		VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.calloc();
		subresourceRange.aspectMask(aspectMask);
		subresourceRange.baseMipLevel(0);
		subresourceRange.levelCount(mipLevels);
		subresourceRange.baseArrayLayer(baseArrayLayer);
		subresourceRange.layerCount(layerCount);
		imageViewCreateInfo.subresourceRange(subresourceRange);
		
		LongBuffer retBuffer = memAllocLong(1);
		int err = vkCreateImageView(device, imageViewCreateInfo, null, retBuffer);
		
		if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to create texture image view: " + err);
		}
		
		imageViewCreateInfo.free();
		subresourceRange.free();
		
		long ret = retBuffer.get(0);
		memFree(retBuffer);
		
		return ret;
	}
	
	protected void copyBuffer(LongBuffer srcBuffer, LongBuffer dstBuffer, long size, LongBuffer commandPool, VkQueue queue) {
		VkCommandBuffer commandBuffer = beginSingleTimeCommands(commandPool);
		
		VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1);
		copyRegion.srcOffset(0L);
		copyRegion.dstOffset(0L);
		copyRegion.size(size);
		
		vkCmdCopyBuffer(commandBuffer, srcBuffer.get(0), dstBuffer.get(0), copyRegion);
		
		endSingleTimeCommands(commandPool, commandBuffer, queue);
		
		copyRegion.free();
	}
	
	protected void copyBufferToImage(LongBuffer buffer, LongBuffer image, int width, int height, LongBuffer commandPool, VkQueue queue) {
		VkCommandBuffer commandBuffer = beginSingleTimeCommands(commandPool);
		
		VkBufferImageCopy.Buffer copyRegion = VkBufferImageCopy.calloc(1);
		copyRegion.bufferOffset(0);
		copyRegion.bufferRowLength(0);
		copyRegion.bufferImageHeight(0);
		VkImageSubresourceLayers subresourceLayers = VkImageSubresourceLayers.calloc();
		subresourceLayers.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
		subresourceLayers.mipLevel(0);
		subresourceLayers.baseArrayLayer(0);
		subresourceLayers.layerCount(1);
		copyRegion.imageSubresource(subresourceLayers);
		VkOffset3D imageOffset = VkOffset3D.calloc().x(0).y(0).z(0);
		copyRegion.imageOffset(imageOffset);
		VkExtent3D imageExtent = VkExtent3D.calloc().width(width).height(height).depth(1);
		copyRegion.imageExtent(imageExtent);
		
		vkCmdCopyBufferToImage(commandBuffer, buffer.get(0), image.get(0), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copyRegion);
		
		endSingleTimeCommands(commandPool, commandBuffer, queue);
		
		copyRegion.free();
		subresourceLayers.free();
		imageOffset.free();
		imageExtent.free();
	}
	
	protected void transitionImageLayout(LongBuffer image, int format, int oldLayout, int newLayout, int mipLevels, LongBuffer commandPool, VkQueue queue) {
		transitionImageLayout(image, format, oldLayout, newLayout, mipLevels, 0, 1, commandPool, queue);
	}
	
	protected void transitionImageLayout(long image, int format, int oldLayout, int newLayout, int mipLevels, int baseArrayLayer, int layerCount, LongBuffer commandPool, VkQueue queue) {
		LongBuffer tmp = memAllocLong(1);
		tmp.put(0, image);
		transitionImageLayout( tmp, format,oldLayout, newLayout, mipLevels, baseArrayLayer, layerCount, commandPool, queue);
		memFree(tmp);
	}
	
	protected void transitionImageLayout(LongBuffer image, int format, int oldLayout, int newLayout, int mipLevels, int baseArrayLayer, int layerCount, LongBuffer commandPool, VkQueue queue) {
		VkCommandBuffer commandBuffer = beginSingleTimeCommands(commandPool);
		
		VkImageMemoryBarrier.Buffer imageMemoryBarriers = VkImageMemoryBarrier.calloc(1);
		imageMemoryBarriers.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
		imageMemoryBarriers.oldLayout(oldLayout);
		imageMemoryBarriers.newLayout(newLayout);
		imageMemoryBarriers.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
		imageMemoryBarriers.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);//We don't want to transfer QueueFamily Ownership
		imageMemoryBarriers.image(image.get(0));
		VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.calloc();
		int aspectMask = 0;
		if(format == VK_FORMAT_D32_SFLOAT || format == VK_FORMAT_D32_SFLOAT_S8_UINT || format == VK_FORMAT_D24_UNORM_S8_UINT) {
			aspectMask |= VK_IMAGE_ASPECT_DEPTH_BIT;
		}else {
			aspectMask |= VK_IMAGE_ASPECT_COLOR_BIT;
		}
		subresourceRange.aspectMask(aspectMask);
		subresourceRange.baseMipLevel(0);
		subresourceRange.levelCount(mipLevels);
		subresourceRange.baseArrayLayer(baseArrayLayer);
		subresourceRange.layerCount(layerCount);
		imageMemoryBarriers.subresourceRange(subresourceRange);
		
		int srcStage;
		int dstStage;
		if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
			imageMemoryBarriers.srcAccessMask(0);
			imageMemoryBarriers.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);//Transfer write wait on nothing
			
			srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;//Start at top of pipe
			dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;//End at transfer stage
		}else if(oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
			imageMemoryBarriers.srcAccessMask(0);
			imageMemoryBarriers.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | 
					VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);//Depth/stencil read write wait on nothing
			
			srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;//Start at transfer stage
			dstStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;//EARLY Tests before Fragment Shader
		}else if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL){
			imageMemoryBarriers.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			imageMemoryBarriers.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);//Shader read wait on transfer write
			
			srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;//Start at transfer stage
			dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;//End at fragment shader
		}else if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL){
			imageMemoryBarriers.srcAccessMask(0);
			imageMemoryBarriers.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);//Color Attachment read/write wait on nothing
			
			srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;//Start at top of pipe
			dstStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;//End at Color attachment output stage
		}else if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_GENERAL) {
			imageMemoryBarriers.srcAccessMask(0);
			imageMemoryBarriers.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_INPUT_ATTACHMENT_READ_BIT);//Color Attachment read/write wait on nothing
			
			srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;//Start at top of pipe
			dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT | VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;//End at Color attachment output stage
		}else if(oldLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL) {
			imageMemoryBarriers.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_INPUT_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_READ_NONCOHERENT_BIT_EXT);
			imageMemoryBarriers.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			
			srcStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT | VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
			dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
		}else if(oldLayout == VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {
			imageMemoryBarriers.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			imageMemoryBarriers.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_INPUT_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_READ_NONCOHERENT_BIT_EXT);
			
			srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
			dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT | VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
		}else if(oldLayout == VK_IMAGE_LAYOUT_PRESENT_SRC_KHR && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
			imageMemoryBarriers.srcAccessMask(0);
			imageMemoryBarriers.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			
			srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
			dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
		}else if(oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_PRESENT_SRC_KHR) {
			imageMemoryBarriers.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			imageMemoryBarriers.dstAccessMask(0);
			
			srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
			dstStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
		}else if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_PRESENT_SRC_KHR) {
			imageMemoryBarriers.srcAccessMask(0);
			imageMemoryBarriers.dstAccessMask(0);
			
			srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
			dstStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
		}else if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_DEPTH_READ_ONLY_STENCIL_ATTACHMENT_OPTIMAL) {
			imageMemoryBarriers.srcAccessMask(0);
			imageMemoryBarriers.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT);
			
			srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
			dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;
		}else {
			throw new IllegalArgumentException("Unsupported layout transition!");
		}
		
		vkCmdPipelineBarrier(commandBuffer, srcStage, dstStage, 0, null, null, imageMemoryBarriers);
		
		endSingleTimeCommands(commandPool, commandBuffer, queue);
		
		imageMemoryBarriers.free();
		subresourceRange.free();
	}
	
	protected VkCommandBuffer beginSingleTimeCommands(LongBuffer commandPool) {//Use to submit several single Time Commands in one Buffer
		VkCommandBuffer ret = createSingleTimeCommandBuffers(commandPool, 1)[0];
		
		VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc();
		commandBufferBeginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
		commandBufferBeginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
			
		int err = vkBeginCommandBuffer(ret, commandBufferBeginInfo);
		
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to begin recording CommandBuffer: " + err);
		}
		
		commandBufferBeginInfo.free();
		
		return ret;
	}
	
	protected VkCommandBuffer beginSingleTimeCommands(LongBuffer commandPool, VkCommandBuffer ret) {//Use to submit several single Time Commands in one Buffer
		VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc();
		commandBufferBeginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
		commandBufferBeginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
			
		int err = vkBeginCommandBuffer(ret, commandBufferBeginInfo);
		
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to begin recording CommandBuffer: " + err);
		}
		
		commandBufferBeginInfo.free();
		
		return ret;
	}
	
	protected VkCommandBuffer[] createSingleTimeCommandBuffers(LongBuffer commandPool, int count) {
		VkCommandBufferAllocateInfo commandBuffersAllocateInfo = VkCommandBufferAllocateInfo.calloc();
		commandBuffersAllocateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
		commandBuffersAllocateInfo.commandPool(commandPool.get(0));
		commandBuffersAllocateInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
		commandBuffersAllocateInfo.commandBufferCount(count);
		
		PointerBuffer commandBufferPointers = memAllocPointer(count);
		int err = vkAllocateCommandBuffers(device, commandBuffersAllocateInfo, commandBufferPointers);
		
		if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to allocate CommandBuffers: " + err);
		}
		
		VkCommandBuffer[] ret = new VkCommandBuffer[count];
		for(int i=0;i<count;++i) {
			ret[i] = new VkCommandBuffer(commandBufferPointers.get(i), device);
		}
		
		memFree(commandBufferPointers);
		commandBuffersAllocateInfo.free();
		
		return ret;
	}
	
	
	
	protected void endSingleTimeCommands(LongBuffer commandPool, VkCommandBuffer commandBuffer, VkQueue queue) {
		int err = vkEndCommandBuffer(commandBuffer);
			
		if (err != VK_SUCCESS) {
	           throw new AssertionError("Failed to record CommandBuffer: " + err);
		}
		
		VkSubmitInfo.Buffer submitInfos = VkSubmitInfo.calloc(1);
		submitInfos.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
		PointerBuffer commandBufferPointer = memAllocPointer(1).put(commandBuffer.address()).flip();
		submitInfos.pCommandBuffers(commandBufferPointer);
		
		VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc();
		fenceCreateInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
		fenceCreateInfo.flags(0);
		
		LongBuffer fence = memAllocLong(1);
		vkCreateFence(device, fenceCreateInfo, null, fence);
		
		err = vkQueueSubmit(queue, submitInfos, fence.get(0));
		
		if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit draw CommandBuffer: " + err);
		}
		
		vkWaitForFences(device, fence, true, -1L);
		
		vkDestroyFence(device, fence.get(0), null);
		vkFreeCommandBuffers(device, commandPool.get(0), commandBuffer);
		
		memFree(fence);
		memFree(commandBufferPointer);
		submitInfos.free();
		fenceCreateInfo.free();
	}
	
	protected void printAvailableExtensions() {
		//Print available Extensions
				int[] extensionCount = new int[1];
				vkEnumerateInstanceExtensionProperties((ByteBuffer)null,extensionCount,null);
				VkExtensionProperties.Buffer extensions = VkExtensionProperties.calloc(extensionCount[0]);
				vkEnumerateInstanceExtensionProperties((ByteBuffer)null,extensionCount,extensions);
				
				System.out.println("\nAvailable Extensions:");
				for(int i=0;i<extensionCount[0];++i) {
					extensions.position(i);
					System.out.println(extensions.extensionNameString());
				}
				extensions.free();
	}
	
	protected void printSupportedValidationLayers() {
		int[] layerCount = new int[1];
		vkEnumerateInstanceLayerProperties(layerCount,null);
		VkLayerProperties.Buffer layers = VkLayerProperties.calloc(layerCount[0]);
		vkEnumerateInstanceLayerProperties(layerCount,layers);
		
		System.out.println("\nSupported ValidationLayers:");
		for(int i=0;i<layerCount[0];++i) {
			layers.position(i);
			System.out.println(layers.layerNameString());
		}
		layers.free();
	}
	
	protected VkQueueFamilyProperties.Buffer getQueueFamilies(VkPhysicalDevice physicalDevice, int[] queueFamilyCount){
		vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);
		VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.calloc(queueFamilyCount[0]);
		vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilies);
		return queueFamilies;
	}
	
	protected VkExtensionProperties.Buffer getAvailableExtensions(int[] extensionCount) {
		vkEnumerateInstanceExtensionProperties((ByteBuffer)null,extensionCount,null);
		VkExtensionProperties.Buffer extensions = VkExtensionProperties.calloc(extensionCount[0]);
		vkEnumerateInstanceExtensionProperties((ByteBuffer)null,extensionCount,extensions);
		return extensions;
	}
	
	protected VkLayerProperties.Buffer getSupportedValidationLayers(int[] layerCount) {
		vkEnumerateInstanceLayerProperties(layerCount,null);
		VkLayerProperties.Buffer layers = VkLayerProperties.calloc(layerCount[0]);
		vkEnumerateInstanceLayerProperties(layerCount,layers);
		return layers;
	}
	
	protected int findMemoryType(int typeFilter, int properties) {
		VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
		vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
		//First don't care about heaps
		for(int i=0;i<memoryProperties.memoryTypeCount();++i) {
			if(((typeFilter & (1 << i))!=0)&&
					((memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties)) {
				memoryProperties.free();
				return i;
			}
		}
		//Check if VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT caused not finding memory; If this is the case, than remove that flag
		if((properties & VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT)!=0) {
			properties = properties & ~VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT;
		}
		for(int i=0;i<memoryProperties.memoryTypeCount();++i) {
			if(((typeFilter & (1 << i))!=0)&&
					((memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties)) {
				memoryProperties.free();
				return i;
			}
		}
		
		memoryProperties.free();
		throw new RuntimeException("Failed to find suitable Memorytype!");
	}
	
	protected int findSupportedFormat(int[] candidates, int tiling, int features) {//candidates orderd from preferred to unpreferred
		for(int i=0;i<candidates.length;++i) {
			VkFormatProperties formatProperties = VkFormatProperties.calloc();
			
			vkGetPhysicalDeviceFormatProperties(physicalDevice, candidates[i], formatProperties);
			
			if(tiling == VK_IMAGE_TILING_LINEAR && (formatProperties.linearTilingFeatures() & features) == features) {
				return candidates[i];
			}else if(tiling == VK_IMAGE_TILING_OPTIMAL && (formatProperties.optimalTilingFeatures() & features) == features) {
				return candidates[i];
			}
			
			formatProperties.free();
		}
		
		throw new RuntimeException("Failed to find supported format!");
	}
	
	protected class debugCallback implements VkDebugUtilsMessengerCallbackEXTI{

		@Override
		public int invoke(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
			VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.createSafe(pCallbackData);
			if(callbackData!=null) {
				if(messageSeverity >= VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) {
					System.err.println(callbackData.pMessageString());
				}else {
					System.out.println(callbackData.pMessageString());
				}
			}
			
			return VK_FALSE;//Always return FALSE...TRUE is reserved
		}
		
	}
	
	protected abstract void mainLoop();
	
	protected abstract void cleanup();
	
	protected abstract void cleanupBuffers();
	
	//Helper
	protected static ByteBuffer String2ByteBuffer(String string, Charset charset) {
		//return ByteBuffer.wrap((string+"\0").getBytes(charset));
		//User memUTF8-Method
		return memUTF8(string);
	}
	
	protected static String ByteBuffer2String(ByteBuffer buffer, Charset charset) {
		//return new String(buffer.array(),charset);
		return memUTF8(buffer);
	}
	
	protected static int log2( int bits ) // returns 0 for bits=0; copyed from StackOverflow
	{
	    int log = 0;
	    if( ( bits & 0xffff0000 ) != 0 ) { bits >>>= 16; log = 16; }
	    if( bits >= 256 ) { bits >>>= 8; log += 8; }
	    if( bits >= 16  ) { bits >>>= 4; log += 4; }
	    if( bits >= 4   ) { bits >>>= 2; log += 2; }
	    return log + ( bits >>> 1 );
	}
	
}
