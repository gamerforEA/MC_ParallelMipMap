package com.gamerforea.parallelmipmap;

import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.io.ByteStreams;

import net.minecraft.launchwrapper.IClassTransformer;

public class ASMTransformer implements IClassTransformer
{
	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes)
	{
		if (transformedName.equals("net.minecraft.client.renderer.texture.TextureMap")) return patchTextureMap(bytes);
		else return bytes;
	}

	private static byte[] patchTextureMap(byte[] basicClass) // net.minecraft.client.renderer.texture.TextureMap
	{
		ClassNode cNode = new ClassNode();
		new ClassReader(basicClass).accept(cNode, 0);

		String loadTextureAtlas = CoreMod.isObfuscated ? "func_110571_b" : "loadTextureAtlas";

		for (int i = 0; i < cNode.methods.size(); i++)
		{
			MethodNode mNode = cNode.methods.get(i);

			if (mNode.name.equals(loadTextureAtlas) && mNode.desc.equals("(Lnet/minecraft/client/resources/IResourceManager;)V"))
			{
				byte[] customBytes = null;

				try (InputStream is = ASMTransformer.class.getClassLoader().getResourceAsStream("net/minecraft/client/renderer/texture/TextureMap.class"))
				{
					customBytes = ByteStreams.toByteArray(is);
				}
				catch (Exception e)
				{
					System.err.println("[ParallelMipMap] Failed loading custom TextureMap.class");
					e.printStackTrace();
					return basicClass;
				}

				ClassNode customNode = new ClassNode();
				new ClassReader(customBytes).accept(customNode, 0);

				for (MethodNode customMNode : customNode.methods)
					if (customMNode.name.equals(loadTextureAtlas) && customMNode.desc.equals("(Lnet/minecraft/client/resources/IResourceManager;)V"))
					{
						cNode.methods.set(i, customMNode);
						break;
					}

				break;
			}
		}

		ClassWriter cWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cNode.accept(cWriter);
		return cWriter.toByteArray();
	}
}