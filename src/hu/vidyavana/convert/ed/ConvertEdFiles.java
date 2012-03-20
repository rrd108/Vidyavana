package hu.vidyavana.convert.ed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ConvertEdFiles
{
	/**
	 * @param args Egyetlen argumentuma a fájl-lista, az indítókönyvtárhoz relatívan.
	 * 		Ennek a fájlnak az első sora a forrásfájlok gyökérkönyvtára,
	 * 		második sora a célfájlok (xml) gyökérkönyvtára,
	 * 		az összes többi sor a forrásfájlok relatív útjai, kezdeti perjel nélkül!
	 * 		A könyvtáraknál az elválasztó karakter lehet / vagy \. A végén opcionális. 
	 * 		Az üres és # kezdetű sorok figyelmen kívül hagyódnak.
	 */
	public static void main(String[] args) throws Exception
	{
		if(args.length < 1)
		{
			System.out.println("Kerlek add meg a fajl nevet, ami a fajlok listajat tartalmazza.");
			return;
		}
		BufferedReader fileList = new BufferedReader(new FileReader(args[0]));
		
		File srcDir = new File(fileList.readLine());
		File destDir = new File(fileList.readLine());
		
		while(true)
		{
			String fname = fileList.readLine();
			if(fname == null) break;
			fname = fname.trim();
			if(fname.length() == 0 || fname.charAt(0)=='#') continue;
			
			File srcFile = new File(srcDir.getAbsolutePath() + "/" + fname);
			File destFile = new File(destDir.getAbsolutePath() + "/" + fname + ".xml");
			
			new EdFileProcessor().process(srcFile, destFile);
		}
		fileList.close();
	}
}
