# app.py
from flask import Flask, request
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@app.route('/')
def get_reversed_ip():
    # Get the client's IP address
    client_ip = request.headers.get('X-Forwarded-For', request.remote_addr)
    
    # Log the original IP
    logger.info(f"Request received from IP: {client_ip}")
    
    # If we have multiple IPs (e.g., from a proxy chain), take the first one
    if ',' in client_ip:
        client_ip = client_ip.split(',')[0].strip()
    
    # Reverse the IP address
    ip_parts = client_ip.split('.')
    reversed_ip = '.'.join(ip_parts[::-1])
    
    # Return the reversed IP
    return f"""
    <html>
    <head>
        <title>IP Reverser</title>
        <style>
            body {{
                font-family: Arial, sans-serif;
                margin: 40px;
                text-align: center;
            }}
            .ip-container {{
                margin: 20px;
                padding: 20px;
                border: 1px solid #ddd;
                border-radius: 5px;
                background-color: #f9f9f9;
            }}
            .original {{
                color: #555;
            }}
            .reversed {{
                color: #0066cc;
                font-size: 1.5em;
                font-weight: bold;
            }}
        </style>
    </head>
    <body>
        <h1>IP Reverser</h1>
        <div class="ip-container">
            <p class="original">Your original IP: {client_ip}</p>
            <p class="reversed">Your IP reversed: {reversed_ip}</p>
        </div>
    </body>
    </html>
    """

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080)
