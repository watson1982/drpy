# coding=utf-8
# !/usr/bin/python
import re
import json
import requests
import time
from lxml import etree
from abc import abstractmethod, ABCMeta
from importlib.machinery import SourceFileLoader
from urllib3 import encode_multipart_formdata


class Spider(metaclass=ABCMeta):  # 元类 默认的元类 type
    _instance = None

    def __init__(self, query_params=None):
        self.query_params = query_params or {}

    def __new__(cls, *args, **kwargs):
        if cls._instance:
            return cls._instance  # 有实例则直接返回
        else:
            cls._instance = super().__new__(cls)  # 没有实例则new一个并保存
            return cls._instance  # 这个返回是给是给init，再实例化一次，也没有关系

    # # 这是简化的写法，上面注释的写法更容易提现判断思路
    # if not cls._instance:               
    #   cls._instance = super().__new__(cls)
    # return cls._instance

    @abstractmethod
    def init_api_ext_file(self):
        pass

    @abstractmethod
    def init(self, extend=""):
        pass

    @abstractmethod
    def homeContent(self, filter):
        pass

    @abstractmethod
    def homeVideoContent(self):
        pass

    @abstractmethod
    def categoryContent(self, tid, pg, filter, extend):
        pass

    @abstractmethod
    def detailContent(self, ids):
        pass

    @abstractmethod
    def searchContent(self, key, quick, pg=1):
        pass

    @abstractmethod
    def playerContent(self, flag, id, vipFlags):
        pass

    @abstractmethod
    def localProxy(self, param):
        pass

    @abstractmethod
    def isVideoFormat(self, url):
        pass

    @abstractmethod
    def manualVideoCheck(self):
        pass

    @abstractmethod
    def getName(self):
        pass

    def getDependence(self):
        return []

    def setExtendInfo(self, extend):
        self.extend = extend

    def regStr(self, src, reg, group=1):
        m = re.search(reg, src)
        src = ''
        if m:
            src = m.group(group)
        return src

    def str2json(self, str):
        return json.loads(str)

    # cGroup = re.compile('[\U00010000-\U0010ffff]')
    # clean = cGroup.sub('',rsp.text)
    def cleanText(self, src):
        clean = re.sub('[\U0001F600-\U0001F64F\U0001F300-\U0001F5FF\U0001F680-\U0001F6FF\U0001F1E0-\U0001F1FF]', '',
                       src)
        return clean

    def fetch(self, url, data=None, headers={}, cookies=""):
        if data is None:
            data = {}
        rsp = requests.get(url, params=data, headers=headers, cookies=cookies)
        rsp.encoding = 'utf-8'
        return rsp

    def post(self, url, data, headers={}, cookies={}):
        rsp = requests.post(url, data=data, headers=headers, cookies=cookies)
        rsp.encoding = 'utf-8'
        return rsp

    def postJson(self, url, json, headers={}, cookies={}):
        rsp = requests.post(url, json=json, headers=headers, cookies=cookies)
        rsp.encoding = 'utf-8'
        return rsp

    def postBinary(self, url, data: dict, boundary=None, headers={}, cookies={}):
        if boundary is None:
            boundary = f'--dio-boundary-{int(time.time())}'
        headers['Content-Type'] = f'multipart/form-data; boundary={boundary}'
        # print(headers)
        fields = []
        for key, value in data.items():
            fields.append((key, (None, value, None)))
        m = encode_multipart_formdata(fields, boundary=boundary)
        data = m[0]
        rsp = requests.post(url, data=data, headers=headers, cookies=cookies)
        rsp.encoding = 'utf-8'
        return rsp

    def html(self, content):
        return etree.HTML(content)

    def xpText(self, root, expr):
        ele = root.xpath(expr)
        if len(ele) == 0:
            return ''
        else:
            return ele[0]

    def loadModule(self, name, fileName):
        return SourceFileLoader(name, fileName).load_module()
