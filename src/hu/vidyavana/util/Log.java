package hu.vidyavana.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class Log
{
	private static Logger logger;
	
	static
	{
		try
		{
			URL url = Log.class.getResource("/hu/resource/logging.properties");
			LogManager.getLogManager().readConfiguration(url.openStream());
			logger = Logger.getLogger("common");

			if(Globals.serverEnv)
			{
				SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmm");
				File f = new File(Globals.cwd, "log/pandit-"+sdf.format(new Date())+".log");
				FileHandler handler = new FileHandler(f.getPath());
				handler.setFormatter(new SimpleFormatter());
				logger.addHandler(handler);
				level(Level.CONFIG);
			}
			else
			{
				ConsoleHandler handler = new ConsoleHandler();
				handler.setFormatter(new SimpleFormatter());
				logger.addHandler(handler);
				level(Level.ALL);
			}
		}
		catch(SecurityException | IOException ex)
		{
			System.out.println("Failed to initialize logging.");
			ex.printStackTrace();
		}
	}
	
	
	public static void level(Level level)
	{
		logger.setLevel(level);
	}
	
	
	public static void error(String text, Throwable t)
	{
		if(text != null)
			logger.severe(text + System.lineSeparator());
		if(t != null)
			logger.severe(stackTrace(t));
	}
	
	
	public static void warning(String text, Throwable t)
	{
		if(text != null)
			logger.warning(text + System.lineSeparator());
		if(t != null)
			logger.warning(stackTrace(t));
	}
	
	
	public static void info(String text)
	{
		logger.info(text + System.lineSeparator());
	}
	
	
	public static void debug(String text)
	{
		logger.finer(text + System.lineSeparator());
	}
	
	
	public static String stackTrace(Throwable t)
	{
		StringWriter sw = new StringWriter(2000);
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.getBuffer().toString();
	}
	
	
	public static void close()
	{
		for(Handler handler : logger.getHandlers())
		{
		    handler.close();
		}
	}
}


class SimpleFormatter extends Formatter
{
	private static final MessageFormat messageFormat = new MessageFormat("[{0,date,MM-dd HH:mm:ss}]: {1}");


	@Override
	public String format(LogRecord record)
	{
		Object[] arguments = new Object[2];
		arguments[0] = new Date(record.getMillis());
		arguments[1] = record.getMessage();
		return messageFormat.format(arguments);
	}

}
