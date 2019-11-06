import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;

import static org.lwjgl.vulkan.VK10.vkCreateCommandPool;


import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

public abstract class VulkanApplicationPerFrameThreadObject<U extends VulkanApplication<? extends VulkanApplicationPerFrameThreadObject<U>>> extends Thread{

	protected final LongBuffer graphicsCommandPools = memAllocLong(3);
	protected final LongBuffer transferCommandPools = memAllocLong(1);
	protected final U parentProcess;
	protected VkCommandBuffer[] commandBuffers;
	
	public VulkanApplicationPerFrameThreadObject(U parentProcess) {
		synchronized(this) {
			this.parentProcess = parentProcess;
		}
	}
	
	protected void createCommandPools() {
		VkCommandPoolCreateInfo graphicsCommandPoolCreateInfo = VkCommandPoolCreateInfo.calloc();
		graphicsCommandPoolCreateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
		graphicsCommandPoolCreateInfo.queueFamilyIndex(parentProcess.queueFamilyIndices[0]);
		graphicsCommandPoolCreateInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
		
		LongBuffer currentCommanPool = memAllocLong(1);
		
		int err;
		for(int i=0;i<graphicsCommandPools.limit();++i) {
			err = vkCreateCommandPool(parentProcess.device, graphicsCommandPoolCreateInfo, null, currentCommanPool);
			
			if (err != VK_SUCCESS) {
	            throw new AssertionError("Failed to create graphicsCommandPool: " + err);
			}
			
			graphicsCommandPools.put(i, currentCommanPool.get(0));
		}
		
		VkCommandPoolCreateInfo transferCommandPoolCreateInfo = VkCommandPoolCreateInfo.calloc();
		transferCommandPoolCreateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
		transferCommandPoolCreateInfo.queueFamilyIndex(parentProcess.queueFamilyIndices[2]);
		transferCommandPoolCreateInfo.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
		
		for(int i=0;i<transferCommandPools.limit();++i) {
			err = vkCreateCommandPool(parentProcess.device, transferCommandPoolCreateInfo, null, currentCommanPool);
			
			if (err != VK_SUCCESS) {
	            throw new AssertionError("Failed to create transferCommandPool: " + err);
			}
			
			transferCommandPools.put(i, currentCommanPool.get(0));
		}
		
		graphicsCommandPoolCreateInfo.free();
		transferCommandPoolCreateInfo.free();
		memFree(currentCommanPool);
	}
	
	protected long[] allocateCommandBuffers() {
		VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc();
		commandBufferAllocateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
		commandBufferAllocateInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
		commandBufferAllocateInfo.commandBufferCount(1);
		
		long[] ret = new long[graphicsCommandPools.limit()];
		
		PointerBuffer currentCommanBufferPointer = memAllocPointer(1);
		int err;
		for(int i=0;i<graphicsCommandPools.limit();++i) {
			commandBufferAllocateInfo.commandPool(graphicsCommandPools.get(i));
			err = vkAllocateCommandBuffers(parentProcess.device, commandBufferAllocateInfo, currentCommanBufferPointer);
			
			if (err != VK_SUCCESS) {
	            throw new AssertionError("Failed to allocate CommandBuffers: " + err);
			}
			
			ret[i] = currentCommanBufferPointer.get(0);

		}
		commandBufferAllocateInfo.free();
		memFree(currentCommanBufferPointer);
		
		return ret;
	}
	
	protected abstract void recordCommandBuffer();
	
	@Override
	public abstract void run();
	
	protected abstract void cleanup();
	
	protected abstract void cleanupBuffers();
}
