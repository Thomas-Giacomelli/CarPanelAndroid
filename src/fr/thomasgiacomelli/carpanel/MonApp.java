package fr.thomasgiacomelli.carpanel;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MonApp extends Activity implements OnClickListener {

	private static final int REQUEST_CODE = 1234;

	private TextView logview;
	private EditText sendtext;
	private Button connect, send, recognizer, phraseMemoire1, phraseMemoire2;

	private BtInterface bt = null;
    
	private long lastTime = 0;
	
	private String t_message = new String();
	
	private boolean sendVoiceCommand = false;
	
	final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            String data = msg.getData().getString("receivedData");
            
            long t = System.currentTimeMillis();
            if(t-lastTime > 100) {// Pour Èviter que les messages soit coupÈs
                logview.append("\n");
				lastTime = System.currentTimeMillis();
			}
            logview.append(data);
        }
    };
    
    final Handler handlerStatus = new Handler() {
        public void handleMessage(Message msg) {
            int co = msg.arg1;
            if(co == 1) {
            	logview.append("Connected\n");
            } else if(co == 2) {
            	logview.append("Disconnected\n");
            }
        }
    };
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        bt = new BtInterface(handlerStatus, handler);
        
        logview = (TextView)findViewById(R.id.logview);
        sendtext = (EditText)findViewById(R.id.sendtxt);

        
        connect = (Button)findViewById(R.id.connect);
        connect.setOnClickListener(this);
        
        send = (Button)findViewById(R.id.send);
        send.setOnClickListener(this);
        send.setEnabled(false);
        
        recognizer = (Button)findViewById(R.id.recognizer);
        recognizer.setOnClickListener(this);
        recognizer.setEnabled(false);
        
        phraseMemoire1 = (Button)findViewById(R.id.phraseMemoire1);
        phraseMemoire1.setOnClickListener(this);
        phraseMemoire1.setEnabled(false);
        
        phraseMemoire2 = (Button)findViewById(R.id.phraseMemoire2);
        phraseMemoire2.setOnClickListener(this);
        phraseMemoire2.setEnabled(false);

        //Listen textview
        sendtext.addTextChangedListener(new TextWatcher()
        {
        	private boolean removeCmd = false;
            public void afterTextChanged(Editable s) 
            {
            	//If not connected to device, can't change edittext datas
            	if (bt.isConnected())
            	{
	            	if (sendVoiceCommand)
	            	{
	            		sendVoiceCommand = false;
	            		
	            		if (removeCmd)
	            		{
	            			removeCmd = false;
	            			sendtext.setText(sendtext.getText().toString().substring(0, sendtext.getText().toString().lastIndexOf(" ")));
	            		}
	            		
	            		logview.setText(new String());
	            		bt.sendData(sendtext.getText().toString().toUpperCase());
	        			sendtext.setText(new String());
	            	}
            	}
            	
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            	t_message = sendtext.getText().toString();
            }
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
            	Log.d("onTextChanged", s.toString());
            	
            	int index = s.toString().lastIndexOf(" ")+1;
            	
            	if ( (s.toString().substring(index).equalsIgnoreCase("envoyer") )  || (s.toString().substring(index).equalsIgnoreCase("envoyé") ) )
            	{
            		sendVoiceCommand = true;
            		removeCmd = true;
            	}
            }
        }); 
        
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0)
        {
            recognizer.setEnabled(false);
            recognizer.setText("Recognizer not present");
        }
    }

	@Override
	public void onClick(View v) {
		if(v == connect) 
		{
			if (!bt.isConnected())
			{
				bt.connect();
				send.setEnabled(true);
				recognizer.setEnabled(true);
				sendtext.setEnabled(true);
				phraseMemoire1.setEnabled(true);
				phraseMemoire2.setEnabled(true);
			}
			
		} 
		else if(v == send) 
		{
			logview.setText(new String());
			bt.sendData(sendtext.getText().toString().toUpperCase());
			sendtext.setText(new String());
		}
		else if(v == recognizer) 
		{
	        startVoiceRecognitionActivity();
		}
		
		else if(v == phraseMemoire1)
		{
			logview.setText(new String());
			bt.sendData("PHRASE NUMERO 1");
			sendtext.setText(new String());
		}
		
		else if(v == phraseMemoire2)
		{
			logview.setText(new String());
			bt.sendData("PHRASE NUMERO 2");
			sendtext.setText(new String());
		}
	}
	
	 /**
     * Fire an intent to start the voice recognition activity.
     */
    private void startVoiceRecognitionActivity()
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice recognition Demo...");
        startActivityForResult(intent, REQUEST_CODE);
    }
    /**
     * Handle the results from the voice recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK)
        {
            // Populate the wordsList with the String values the recognition engine thought it heard
        	ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        	
        	//Separer les mots de la chaine
        	String[] tab = matches.get(0).split(" ");
            int lenght = tab.length;
            System.out.println("Premier mot ["+tab[0]+"]\n Dernier mot ["+tab[lenght-1]+"]\n nombre de mots ["+lenght+"]\n");
        	
        	if ((tab[lenght - 1].equalsIgnoreCase("envoyer")) || (tab[lenght - 1].equalsIgnoreCase("envoyé")))
        	{
        		String t_s = new String();
        		
        		int i = 0;
        		for (i = 0; i < lenght - 1; i++)
        		{
        			t_s += tab[i] + " ";
        		}
        		
        		matches.set(0, t_s);
        		
        		sendVoiceCommand = true;
        	}
        	sendtext.setText(matches.get(0));
        
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	//bt.close();
    }
}