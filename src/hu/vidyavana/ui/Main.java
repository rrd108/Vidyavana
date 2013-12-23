package hu.vidyavana.ui;

import hu.vidyavana.db.*;
import hu.vidyavana.db.api.*;
import hu.vidyavana.db.model.*;
import hu.vidyavana.ui.model.VDocument;
import hu.vidyavana.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.BevelBorder;
import javax.swing.text.*;
import org.apache.lucene.index.IndexWriter;
import com.sleepycat.persist.*;

public class Main implements UncaughtExceptionHandler
{
	public static Main inst;
	public ExecutorService executor;

	public JFrame frame;
	private JMenuBar menuBar;
	private JToolBar toolBar;
	public VDocument doc;
	public JTextPane textPane;


	public static void main(String[] args)
	{
		setLookAndFeel();

		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				inst = new Main();
				inst.main();
			}
		});
	}


	void main()
	{
		Thread.setDefaultUncaughtExceptionHandler(this);
		System.setProperty("sun.awt.exception.handler", getClass().getName());

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					executor.shutdown();
					executor.awaitTermination(5, TimeUnit.SECONDS);
				}
				catch(Exception ex)
				{
				}
				Db.inst.close();
				Lucene.inst.close();
				Log.close();
			}
		});

		executor = Executors.newSingleThreadExecutor();
		databaseMigration();
		Encrypt.getInstance().init();
		showWindow();
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				SplashScreen sp = SplashScreen.getSplashScreen();
				if(sp != null)
					sp.close();
			}
		});
	}


	@Override
	public void uncaughtException(Thread th, Throwable t)
	{
		Log.error(th.getName(), t);
		while(t.getCause() != null && t.getCause() != t)
			t = t.getCause();
		String msg = t.getMessage();
		if(msg == null || msg.isEmpty())
			msg = t.getClass().getName();
		try
		{
			messageBox(msg, "Hiba");
		}
		catch(Exception e)
		{
			System.out.println(msg);
		}
	}


	private void databaseMigration()
	{
		Db.openForWrite();
		PrimaryIndex<String, Settings> settings = Settings.pkIdx();
		EntityCursor<Settings> crsr = settings.entities();
		Settings rec = crsr.first();
		crsr.close();
		if(rec == null)
		{
			rec = new Settings();
			rec.dbMigrate = "0";
			rec.booksVersion = "0";
			settings.put(rec);
		}
		Db.openForRead();
	}


	private static void setLookAndFeel()
	{
		try
		{
			boolean hasNimbus = false;
			for(LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
			{
				if("Nimbus".equals(info.getName()))
				{
					UIManager.setLookAndFeel(info.getClassName());
					hasNimbus = true;
					break;
				}
			}
			if(!hasNimbus)
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e)
		{
		}
	}


	private void showWindow()
	{
		menuBar = new JMenuBar();
		menuBar.setBorder(new BevelBorder(BevelBorder.RAISED));

		addDatabaseMenu();

		toolBar = new JToolBar();
		// toolBar.setBorder(new EtchedBorder());
		toolBar.add(new JButton("Placeholder"));

		UIManager.put("TextPane.font", new Font("Times New Roman", Font.PLAIN, 24));  
		textPane = new JTextPane();
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(textPane);

		frame = new JFrame("Vidyāvana");
		setIcon(frame);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setJMenuBar(menuBar);
		frame.add(scrollPane);
		frame.add(toolBar, BorderLayout.NORTH);
		frame.setSize(1024, 600);
		frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setVisible(true);
	}


	private void setIcon(JFrame frame)
	{
		try
		{
			Image im = ImageIO.read(getClass().getResource("/hu/resource/image/vidyavana.gif"));
			frame.setIconImage(im);
		}
		catch(IOException ex)
		{
		}
	}


	public static void messageBox(final String msg, final String caption)
	{
		if(SwingUtilities.isEventDispatchThread())
			messageBoxOnSwingThread(msg, caption);
		else
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					messageBoxOnSwingThread(msg, caption);
				}
			});
	}


	static void messageBoxOnSwingThread(String msg, String caption)
	{
		JOptionPane.showMessageDialog(Main.inst.frame, msg, caption, JOptionPane.PLAIN_MESSAGE);
	}


	private void addDatabaseMenu()
	{
		JMenu dbMenuItem = new JMenu("Database");
		menuBar.add(dbMenuItem);

		dbMenuItem.add(new JMenuItem(new UpdateBooksAction()));
		dbMenuItem.add(new JMenuItem(new AddBooksAction()));
		dbMenuItem.add(new JMenuItem(new TestAction()));
	}


	private final class UpdateBooksAction extends AbstractAction
	{
		public UpdateBooksAction()
		{
			super("Update Books");
		}


		@Override
		public void actionPerformed(ActionEvent e)
		{
			executor.execute(new Runnable()
			{
				@Override
				public void run()
				{
					UpdateBooks ub = new UpdateBooks();
					ub.run();
					messageBox("Új: " + ub.added + ", módosítva: " + ub.updated, "Eredmény");
				}
			});
		}
	}


	private final class AddBooksAction extends AbstractAction
	{
		public AddBooksAction()
		{
			super("Add Books");
		}


		@Override
		public void actionPerformed(ActionEvent e)
		{
			executor.execute(new Runnable()
			{
				@Override
				public void run()
				{
					String[] paths = {
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\bg"/*,
						"",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\SB  1",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\SB  2",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\SB  3",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\SB  4",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\SB  5",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\SB  6",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\SB  7",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\SB  8",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\SB  9",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\SB 10",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\kb",
						"",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\cc-adi",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\cc-madhya",
						"d:\\wk2\\Sastra\\BBT\\v2012\\xml\\cc-antya" */
						
					};
					IndexWriter writer = Lucene.inst.open().writer();
					for(int bookId = 1; bookId <= paths.length; ++bookId)
					{
						if(bookId == 2 || bookId == 14)
							continue;
						System.out.println(bookId);
						new AddBook(bookId, paths[bookId - 1], writer).run();
					}
					Lucene.inst.closeWriter();
					messageBox("Könyv(ek) hozzáadva.", "Kész");
				}
			});
		}
	}


	private final class TestAction extends AbstractAction
	{
		public TestAction()
		{
			super("Test code");
		}


		@Override
		public void actionPerformed(ActionEvent e)
		{
			executor.execute(new Runnable()
			{
				@Override
				public void run()
				{
					Timing.start();
					Timing.start();
					BookOrdinalKey start = new BookOrdinalKey(1, 0);
					BookOrdinalKey end = new BookOrdinalKey(1, 100);
					EntityCursor<Para> psCur = Para.pkIdx().entities(start, true, end, false);
					Encrypt enc = Encrypt.getInstance();
					Para p;
					AttributeSet a = SimpleAttributeSet.EMPTY;
					//StringBuilder sb = new StringBuilder(100000);
					doc = new VDocument();
					doc.startBatchInsert();
					while((p=psCur.next()) != null)
					{
						String txt = enc.decrypt(p.text);
						//sb.append(txt);
						//sb.append('\n');
						doc.append(txt.toCharArray(), a);
						doc.appendPara(a);
					}
					psCur.close();
					try
					{
//						StringContent content = new StringContent(100000);
//						content.insertString(0, sb.toString());
//						System.out.println(content.length());
//						doc = new DefaultStyledDocument(content, new StyleContext());
						doc.endBatchInsert(0);
//						textPane.setEditorKit(new VEditorKit());
						Timing.stop("Before setting document");
						textPane.setDocument(doc);
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}
					Timing.stop("Show Bg");
					// messageBox("Loaded", "Msg");
				}
			});
		}
	}
}
